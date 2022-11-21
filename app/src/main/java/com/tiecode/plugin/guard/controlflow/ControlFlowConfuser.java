package com.tiecode.plugin.guard.controlflow;

import com.tiecode.platform.compiler.api.descriptor.Name;
import com.tiecode.platform.compiler.api.file.TiecodeFileObject;
import com.tiecode.platform.compiler.source.tree.Tree;
import com.tiecode.platform.compiler.toolchain.env.CompilationEnv;
import com.tiecode.platform.compiler.toolchain.env.Context;
import com.tiecode.platform.compiler.toolchain.parser.TiecodeToken;
import com.tiecode.platform.compiler.toolchain.processor.NameGenerator;
import com.tiecode.platform.compiler.toolchain.tree.TCTree;
import com.tiecode.platform.compiler.toolchain.tree.TCTreeScanner;
import com.tiecode.platform.compiler.toolchain.tree.TreeMaker;
import com.tiecode.platform.compiler.toolchain.tree.TreeModifier;
import com.tiecode.platform.compiler.toolchain.tree.type.Type;
import com.tiecode.platform.compiler.toolchain.tree.type.Types;
import com.tiecode.platform.compiler.toolchain.util.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * 控制流混淆器
 * 本混淆器仅执行目前市面上主流的三个规则（扁平化、不透明化、加入多余控制流）
 *
 * @author Scave
 */
public final class ControlFlowConfuser extends TreeModifier<Void> {
    private final TreeMaker maker;
    private final NameGenerator nameGenerator;
    private final Types types;
    private final CompilationEnv env;
    private final Stack<TCTree.TCMethodDeclare> enclMethods = new Stack<>();

    public ControlFlowConfuser(Context context) {
        this.maker = new TreeMaker();
        this.nameGenerator = context.get(NameGenerator.key);
        this.types = Types.instance(context);
        this.env = CompilationEnv.instance(context);
    }

    public static int random(int min, int max) {
        Random random = new Random();
        int res = random.nextInt(max) % (max - min + 1) + min;
        return res;
    }

    public static boolean down() {
        return System.nanoTime() % 2 == 0;
    }

    public static int[] randOrders(int count, int standard, int breakN) {
        int down = standard - 1;
        int up = standard + 1;
        int[] result = new int[count];
        for (int i = 0; i < count; i++) {
            int n = breakN;
            while (n == breakN) {
                if (down()) {
                    n = down;
                    down--;
                } else {
                    n = up;
                    up++;
                }
            }
            result[i] = n;
        }
        for (int i = 0; i < result.length; i++) {
            for (int j = i; j < result.length; j++) {
                if (down()) {
                    int tmp = result[i];
                    result[i] = result[j];
                    result[j] = tmp;
                }
            }
        }
        return result;
    }

    @Override
    public TCTree visitBlock(TCTree.TCBlock tree, Void param) {
        flatten(tree);
        return tree;
    }

    @Override
    public TCTree visitEvent(TCTree.TCEvent tree, Void param) {
        enclMethods.push(tree);
        VariableScanner scanner = new VariableScanner();
        scanner.scan(tree, null);
        flatten(tree.block);
        declareMethodVariables(tree, scanner.variables);
        addMethodReturn(tree);
        enclMethods.pop();
        return tree;
    }

    @Override
    public TCTree visitMethod(TCTree.TCMethodDeclare tree, Void param) {
        if (tree.block == null || tree.modifiers.isAsync()) {
            return tree;
        }
        enclMethods.push(tree);
        VariableScanner scanner = new VariableScanner();
        scanner.scan(tree, null);
        flatten(tree.block);
        declareMethodVariables(tree, scanner.variables);
        addMethodReturn(tree);
        enclMethods.pop();
        return tree;
    }

    @Override
    public TCTree visitReturn(TCTree.TCReturn tree, Void param) {
        TCTree.TCMethodDeclare method = enclMethods.peek();
        TCTree.TCIdentifier identifier = maker.identifier(method.name);
        TCTree.TCAssignment assign = maker.assign(identifier, tree.expression);
        return assign;
    }

    @Override
    public TCTree visitVariable(TCTree.TCVariableDeclare tree, Void param) {
        TCTree.TCIdentifier identifier = maker.identifier(tree.name);
        TCTree.TCAssignment assign = maker.assign(identifier, tree.initializer);
        TCTree.TCExpressionStatement statement = maker.expressionStatement(assign);
        return statement;
    }

    private void flatten(TCTree.TCBlock block) {
        if (block == null || block.statements == null) {
            return;
        }
        int n = random(1, 9);
        int addN = random(1, 5);
        int breakN = n + addN;
        TCTree.TCVariableDeclare randomVar = makeRandomVar(n);
        TCTree.TCWhileLoop loop = makeWhile(randomVar, n, addN);
        TCTree.TCSWitch aSwitch = makeSwitch(randomVar);
        for (int i = 0; i < block.statements.size(); i++) {
            block.statements.set(i, modify(block.statements.get(i)));
            if (down()) {
                TCTree.TCStatement newSt = makeOpacityCode(block.statements.get(i));
                block.statements.set(i, newSt);
            }
            if (down()) {
                TCTree.TCStatement junkCode = makeJunkCode();
                block.statements.add(i, junkCode);
                i++;
            }
        }
        int count = block.statements.size();
        int[] orders = randOrders(count, n, breakN);
        CaseModel[] models = new CaseModel[count];
        for (int i = 0, len = orders.length; i < len; i++) {
            TCTree.TCStatement statement = block.statements.get(i);
            int order = orders[i];
            int nextOrder = -1;
            if (i < orders.length - 1) {
                nextOrder = orders[i + 1];
            }
            CaseModel model = new CaseModel(order, nextOrder, statement);
            models[i] = model;
        }
        if (count == 0) {
            return;
        }
        models[0].order = n;
        models[count - 1].nextOrder = breakN;
        fillCases(randomVar, aSwitch, models);
        loop.block.statements.add(aSwitch);
        block.statements.clear();
        block.statements.add(randomVar);
        block.statements.add(loop);
    }

    private TCTree.TCStatement makeJunkCode() {
        //判断不透明化指令加入哪边
        boolean down = down();
        TCTree.TCMethodInvocation tip = maker.methodInvocation(Names.of("取存储卡路径"), null, null);
        TCTree.TCExpressionStatement first = maker.expressionStatement(tip);
        int n1 = random(1, 100);
        int n2 = random(1, 100);
        TCTree.TCLiteral num1 = maker.literal(n1, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCLiteral num2 = maker.literal(n2, TiecodeToken.INTEGER_LITERAL);
        List<TCTree.TCExpression> args = new ArrayList<>();
        args.add(num1);
        args.add(num2);
        TCTree.TCMethodInvocation getPath = maker.methodInvocation(Names.of("取随机数"), null, args);
        TCTree.TCExpressionStatement second = maker.expressionStatement(getPath);

        TCTree.TCMethodInvocation invoke = maker.methodInvocation(Names.of("取当前时间戳"), null, null);
        TCTree.TCLiteral two = maker.literal(2, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCLiteral zero = maker.literal(0, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCBinary binary = maker.binary(invoke, TiecodeToken.MOD, two);
        TCTree.TCTypeCast cast = maker.typeCast(binary, maker.identifier(Names.INT));
        TCTree.TCBinary cond;
        if (down) {
            cond = maker.binary(cast, TiecodeToken.GTEQ, zero);
        } else {
            cond = maker.binary(cast, TiecodeToken.LT, zero);
        }
        List<TCTree.TCStatement> thenPart = new ArrayList<>();
        if (down) {
            thenPart.add(first);
        } else {
            thenPart.add(second);
        }
        TCTree.TCBlock thenBlock = maker.block(thenPart);
        List<TCTree.TCStatement> elsePart = new ArrayList<>();
        if (down) {
            elsePart.add(second);
        } else {
            elsePart.add(first);
        }
        TCTree.TCBlock elseBock = maker.block(elsePart);
        TCTree.TCIf ifSt = maker.ifSt(cond, thenBlock, elseBock);
        return ifSt;
    }

    private TCTree.TCStatement makeOpacityCode(TCTree.TCStatement tree) {
        //判断不透明化指令加入哪边
        boolean down = down();
        TCTree.TCLiteral literal = maker.literal("", TiecodeToken.STRING);
        List<TCTree.TCExpression> args = new ArrayList<>();
        args.add(literal);
        TCTree.TCMethodInvocation tip = maker.methodInvocation(Names.of("弹出提示"), null, args);
        TCTree.TCExpressionStatement second = maker.expressionStatement(tip);

        TCTree.TCMethodInvocation invoke = maker.methodInvocation(Names.of("取当前时间戳"), null, null);
        TCTree.TCLiteral two = maker.literal(2, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCLiteral zero = maker.literal(0, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCBinary binary = maker.binary(invoke, TiecodeToken.MOD, two);
        TCTree.TCTypeCast cast = maker.typeCast(binary, maker.identifier(Names.INT));
        TCTree.TCBinary cond;
        if (down) {
            cond = maker.binary(cast, TiecodeToken.GTEQ, zero);
        } else {
            cond = maker.binary(cast, TiecodeToken.LT, zero);
        }
        List<TCTree.TCStatement> thenPart = new ArrayList<>();
        if (down) {
            thenPart.add(tree);
        } else {
            thenPart.add(second);
        }
        TCTree.TCBlock thenBlock = maker.block(thenPart);
        List<TCTree.TCStatement> elsePart = new ArrayList<>();
        if (down) {
            elsePart.add(second);
        } else {
            elsePart.add(tree);
        }
        TCTree.TCBlock elseBock = maker.block(elsePart);
        TCTree.TCIf ifSt = maker.ifSt(cond, thenBlock, elseBock);
        return ifSt;
    }

    private void declareMethodVariables(TCTree.TCMethodDeclare method, List<TCTree.TCVariableDeclare> variables)  {
        TCTree.TCBlock block = method.block;
        for (int i = variables.size() - 1; i >= 0; i--) {
            TCTree.TCVariableDeclare var = variables.get(i);
            modifyInitializer(var);
            var.type = maker.makeType(env, var.symbol.type);
            method.block.statements.add(0, var);
        }
        if (method.returnType == null) {
            return;
        }
        Type type = method.getSymbol().type;
        TCTree.TCTypeExpression expression = maker.makeType(env, type);
        TCTree.TCVariableDeclare returnVar = maker.variable(method.name, null, expression, null);
        modifyInitializer(returnVar);
        block.statements.add(returnVar);
    }

    private void modifyInitializer(TCTree.TCVariableDeclare variable) {
        Type type = variable.symbol.type;
        if (types.isNumericType(type)) {
            TCTree.TCLiteral literal = maker.literal(0, TiecodeToken.INTEGER_LITERAL);
            variable.initializer = literal;
        } else if (type == types.BOOL) {
            TCTree.TCLiteral literal = maker.literal(false, TiecodeToken.FALSE);
            variable.initializer = literal;
        } else if (type == types.CHAR) {
            TCTree.TCLiteral literal = maker.literal(' ', TiecodeToken.CHARACTER_LITERAL);
            variable.initializer = literal;
        } else {
            TCTree.TCLiteral literal = maker.literal(null, TiecodeToken.NULL);
            variable.initializer = literal;
        }
    }

    private void addMethodReturn(TCTree.TCMethodDeclare method) {
        if (method.returnType == null) {
            return;
        }
        TCTree.TCIdentifier identifier = maker.identifier(method.name);
        TCTree.TCReturn returnExpr = maker.returnExpr(identifier);
        TCTree.TCExpressionStatement statement = maker.expressionStatement(returnExpr);
        method.block.statements.add(statement);
    }

    private TCTree.TCVariableDeclare makeRandomVar(int n) {
        TCTree.TCLiteral n1 = maker.literal(n, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCLiteral n2 = maker.literal(n, TiecodeToken.INTEGER_LITERAL);
        List<TCTree.TCExpression> args = new ArrayList<>();
        args.add(n1);
        args.add(n2);
        TCTree.TCMethodInvocation invocation = maker.methodInvocation(Names.of("取随机数"), null, args);
        Name varName = Names.of(nameGenerator.randName());
        TCTree.TCTypeExpression type = maker.identifier(Names.INT);
        TCTree.TCVariableDeclare variable = maker.variable(varName, null, type, invocation);
        return variable;
    }

    private TCTree.TCWhileLoop makeWhile(TCTree.TCVariableDeclare randomVar, int n, int addN) {
        TCTree.TCIdentifier left = maker.identifier(randomVar.name);
        TCTree.TCLiteral right = maker.literal(n + addN, TiecodeToken.INTEGER_LITERAL);
        TCTree.TCBinary binary = maker.binary(left, TiecodeToken.NOTEQ, right);
        List<TCTree.TCStatement> statements = new ArrayList<>();
        TCTree.TCBlock block = maker.block(statements);
        TCTree.TCWhileLoop loop = maker.whileLoop(binary, block);
        return loop;
    }

    private TCTree.TCSWitch makeSwitch(TCTree.TCVariableDeclare randomVar) {
        TCTree.TCIdentifier condition = maker.identifier(randomVar.name);
        List<TCTree.TCCase> cases = new ArrayList<>();
        TCTree.TCSWitch aSwitch = maker.switchSt(condition, cases);
        return aSwitch;
    }

    private void fillCases(TCTree.TCVariableDeclare randomVar, TCTree.TCSWitch aSwitch, CaseModel[] models) {
        for (CaseModel model : models) {
            TCTree.TCLiteral order = maker.literal(model.order, TiecodeToken.INTEGER_LITERAL);
            List<TCTree.TCStatement> statements = new ArrayList<>();
            statements.add(model.statement);
            TCTree.TCIdentifier varName = maker.identifier(randomVar.name);
            TCTree.TCLiteral nextOrder = maker.literal(model.nextOrder, TiecodeToken.INTEGER_LITERAL);
            TCTree.TCAssignment orderChange = maker.assign(varName, nextOrder);
            TCTree.TCExpressionStatement changeSt = maker.expressionStatement(orderChange);
            statements.add(changeSt);
            TCTree.TCBlock block = maker.block(statements);
            TCTree.TCCase aCase = maker.caseCondition(order, block);
            aSwitch.cases.add(aCase);
        }
    }

    static class CaseModel {
        int order;
        int nextOrder;
        TCTree.TCStatement statement;

        public CaseModel(int order, int nextOrder, TCTree.TCStatement statement) {
            this.order = order;
            this.nextOrder = nextOrder;
            this.statement = statement;
        }
    }

    static class VariableScanner extends TCTreeScanner<Void, Void> {
        List<TCTree.TCVariableDeclare> variables = new ArrayList<>();

        @Override
        public Void visitVariable(TCTree.TCVariableDeclare tree, Void unused) {
            if (!tree.isParameter) {
                variables.add(tree);
            }
            return null;
        }
    }
}
