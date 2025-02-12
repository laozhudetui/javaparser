/*
 * Copyright (C) 2015-2016 Federico Tomassetti
 * Copyright (C) 2017-2020 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.symbolsolver.javaparsermodel;

import static com.github.javaparser.symbolsolver.javaparser.Navigator.demandParentNode;
import static com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade.solveGenericTypes;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedVoidType;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import com.github.javaparser.symbolsolver.logic.FunctionalInterfaceLogic;
import com.github.javaparser.symbolsolver.logic.InferenceContext;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.resolution.Value;
import com.github.javaparser.symbolsolver.model.typesystem.NullType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.symbolsolver.reflectionmodel.MyObjectProvider;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typeinference.TypeHelper;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.Pair;
import com.google.common.collect.ImmutableList;

public class TypeExtractor extends DefaultVisitorAdapter {

    private TypeSolver typeSolver;
    private JavaParserFacade facade;

    public TypeExtractor(TypeSolver typeSolver, JavaParserFacade facade) {
        this.typeSolver = typeSolver;
        this.facade = facade;
    }

    @Override
    public ResolvedType visit(VariableDeclarator node, Boolean solveLambdas) {
        if (demandParentNode(node) instanceof FieldDeclaration) {
            return facade.convertToUsageVariableType(node);
        } else if (demandParentNode(node) instanceof VariableDeclarationExpr) {
            return facade.convertToUsageVariableType(node);
        }
        throw new UnsupportedOperationException(demandParentNode(node).getClass().getCanonicalName());
    }

    @Override
    public ResolvedType visit(Parameter node, Boolean solveLambdas) {
        if (node.getType() instanceof UnknownType) {
            throw new IllegalStateException("Parameter has unknown type: " + node);
        }
        return facade.convertToUsage(node.getType(), node);
    }


    @Override
    public ResolvedType visit(ArrayAccessExpr node, Boolean solveLambdas) {
        ResolvedType arrayUsageType = node.getName().accept(this, solveLambdas);
        if (arrayUsageType.isArray()) {
            return ((ResolvedArrayType) arrayUsageType).getComponentType();
        }
        return arrayUsageType;
    }

    @Override
    public ResolvedType visit(ArrayCreationExpr node, Boolean solveLambdas) {
        ResolvedType res = facade.convertToUsage(node.getElementType(), JavaParserFactory.getContext(node, typeSolver));
        for (int i = 0; i < node.getLevels().size(); i++) {
            res = new ResolvedArrayType(res);
        }
        return res;
    }

    @Override
    public ResolvedType visit(ArrayInitializerExpr node, Boolean solveLambdas) {
        throw new UnsupportedOperationException(node.getClass().getCanonicalName());
    }

    @Override
    public ResolvedType visit(AssignExpr node, Boolean solveLambdas) {
        return node.getTarget().accept(this, solveLambdas);
    }

    @Override
    public ResolvedType visit(BinaryExpr node, Boolean solveLambdas) {
        switch (node.getOperator()) {
            case PLUS:
            case MINUS:
            case DIVIDE:
            case MULTIPLY:
            case REMAINDER:
            case BINARY_AND:
            case BINARY_OR:
            case XOR:
                return facade.getBinaryTypeConcrete(node.getLeft(), node.getRight(), solveLambdas, node.getOperator());
            case LESS_EQUALS:
            case LESS:
            case GREATER:
            case GREATER_EQUALS:
            case EQUALS:
            case NOT_EQUALS:
            case OR:
            case AND:
                return ResolvedPrimitiveType.BOOLEAN;
            case SIGNED_RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
            case LEFT_SHIFT:
                ResolvedType rt = node.getLeft().accept(this, solveLambdas);
                // apply unary primitive promotion
                return ResolvedPrimitiveType.unp(rt);
            default:
                throw new UnsupportedOperationException("Operator " + node.getOperator().name());
        }
    }

    @Override
    public ResolvedType visit(CastExpr node, Boolean solveLambdas) {
        return facade.convertToUsage(node.getType(), JavaParserFactory.getContext(node, typeSolver));
    }

    @Override
    public ResolvedType visit(ClassExpr node, Boolean solveLambdas) {
        // This implementation does not regard the actual type argument of the ClassExpr.
        Type astType = node.getType();
        ResolvedType jssType = facade.convertToUsage(astType, node.getType());
        return new ReferenceTypeImpl(new ReflectionClassDeclaration(Class.class, typeSolver), ImmutableList.of(jssType), typeSolver);
    }

    /*
     * The conditional operator has three operand expressions. ? appears between the first and second expressions, and
     * : appears between the second and third expressions.
     * There are three kinds of conditional expressions, classified according to the second and third operand
     * expressions: boolean conditional expressions, numeric conditional expressions, and reference conditional
     * expressions.
     * The classification rules are as follows:
     * 1/ If both the second and the third operand expressions are boolean expressions, the conditional expression is a
     * boolean conditional expression.
     * 2/ If both the second and the third operand expressions are numeric expressions, the conditional expression is a
     * numeric conditional expression.
     * 3/ Otherwise, the conditional expression is a reference conditional expression
     */
    @Override
    public ResolvedType visit(ConditionalExpr node, Boolean solveLambdas) {
        ResolvedType thenExpr = node.getThenExpr().accept(this, solveLambdas);
        ResolvedType elseExpr = node.getElseExpr().accept(this, solveLambdas);
        
        // manage null expression
        if ( thenExpr.isNull()) {
            return  elseExpr;
        }
        if ( elseExpr.isNull()) {
            return  thenExpr;
        }
        /*
         * Boolean conditional expressions are standalone expressions
         * The type of a boolean conditional expression is determined as follows:
         * If the second and third operands are both of type Boolean, the conditional expression has type Boolean.
         * Otherwise, the conditional expression has type boolean.
         */
        if ( thenExpr.isAssignableBy(ResolvedPrimitiveType.BOOLEAN) 
                && elseExpr.isAssignableBy(ResolvedPrimitiveType.BOOLEAN)) {
            if (thenExpr.isReferenceType() && elseExpr.isReferenceType()) {
                return thenExpr.asReferenceType();
            }
            return thenExpr.isPrimitive() ? thenExpr : elseExpr;
        }
        
        /*
         * Numeric conditional expressions are standalone expressions (§15.2).
         * The type of a numeric conditional expression is determined as follows:
         * If the second and third operands have the same type, then that is the type of the conditional expression.
         * If one of the second and third operands is of primitive type T, and the type of the other is the result of
         * applying boxing conversion (§5.1.7) to T, then the type of the conditional expression is T.
         * If one of the operands is of type byte or Byte and the other is of type short or Short, then the type of the
         * conditional expression is short.
         * If one of the operands is of type T where T is byte, short, or char, and the other operand is a constant
         * expression (§15.28) of type int whose value is representable in type T, then the type of the conditional
         * expression is T.
         * If one of the operands is of type T, where T is Byte, Short, or Character, and the other operand is a
         * constant expression of type int whose value is representable in the type U which is the result of applying
         * unboxing conversion to T, then the type of the conditional expression is U.
         * Otherwise, binary numeric promotion (§5.6.2) is applied to the operand types, and the type of the
         * conditional expression is the promoted type of the second and third operands.
         */
        if (thenExpr.isNumericType() && elseExpr.isNumericType()) {
            ResolvedPrimitiveType[] resolvedPrimitiveTypeSubList = new ResolvedPrimitiveType[] {ResolvedPrimitiveType.BYTE, ResolvedPrimitiveType.SHORT, ResolvedPrimitiveType.CHAR};
            /*
             *  If the second and third operands have the same type, then that is the type of the conditional expression.
             */
            String qnameTypeThenExpr = thenExpr.isPrimitive() ? thenExpr.asPrimitive().describe()
                    : thenExpr.asReferenceType().describe();
            String qnameTypeElseExpr = elseExpr.isPrimitive() ? elseExpr.asPrimitive().describe()
                    : elseExpr.asReferenceType().describe();
            if (qnameTypeThenExpr.equals(qnameTypeElseExpr)) {
                return thenExpr;
            }
            /*
             * If one of the second and third operands is of primitive type T, and the type of the other is the result of
             * applying boxing conversion (§5.1.7) to T, then the type of the conditional expression is T.
             */
            else if ((thenExpr.isPrimitive() && elseExpr.isReferenceType()
                    && isCompatible(elseExpr.asReferenceType(), thenExpr.asPrimitive()))) {
                return thenExpr;
            } else if ((elseExpr.isPrimitive() && thenExpr.isReferenceType()
                    && isCompatible(thenExpr.asReferenceType(), elseExpr.asPrimitive()))) {
                return elseExpr;
            }
            /*
             * If one of the operands is of type byte or Byte and the other is of type short or Short, then the type of the
             * conditional expression is short.
             */
            else if ((isCompatible(thenExpr, ResolvedPrimitiveType.BYTE) && isCompatible(elseExpr, ResolvedPrimitiveType.SHORT))
                    || (isCompatible(elseExpr, ResolvedPrimitiveType.BYTE) && isCompatible(thenExpr, ResolvedPrimitiveType.SHORT))) {
                return ResolvedPrimitiveType.SHORT;
            }
            /*
             *  If one of the operands is of type T where T is byte, short, or char, and the
             *  other operand is a constant expression (§15.28) of type int whose value is
             *  representable in type T, then the type of the conditional expression is T
             *  How can we know if the constant expression of type int is representable in type T ?
             *  "The constant expression of type int is representable in type T" is a runtime decision!
             */
            else if (thenExpr.isPrimitive() && elseExpr.isPrimitive()) {
                if (((ResolvedPrimitiveType)thenExpr).in(resolvedPrimitiveTypeSubList)
                    && ((ResolvedPrimitiveType)elseExpr).equals(ResolvedPrimitiveType.INT)) {
                    return thenExpr;
                } else if (((ResolvedPrimitiveType)elseExpr).in(resolvedPrimitiveTypeSubList)
                    && ((ResolvedPrimitiveType)thenExpr).equals(ResolvedPrimitiveType.INT)) {
                    return elseExpr;
                }
            }
             /*  If one of the operands is of type T, where T is Byte, Short, or Character,
             * and the other operand is a constant expression of type int whose value is
             * representable in the type U which is the result of applying unboxing
             * conversion to T, then the type of the conditional expression is U.
             * A priori this is a runtime decision!
             */
            else if (thenExpr.isReference() && elseExpr.isPrimitive()
                    && thenExpr.asReferenceType().isUnboxable()
                    && thenExpr.asReferenceType().toUnboxedType().get().in(resolvedPrimitiveTypeSubList)
                    && ((ResolvedPrimitiveType)elseExpr).equals(ResolvedPrimitiveType.INT)) {
                return thenExpr.asReferenceType().toUnboxedType().get();
            } else if (elseExpr.isReference() && thenExpr.isPrimitive()
                    && elseExpr.asReferenceType().isUnboxable()
                    && elseExpr.asReferenceType().toUnboxedType().get().in(resolvedPrimitiveTypeSubList)
                    && ((ResolvedPrimitiveType)thenExpr).equals(ResolvedPrimitiveType.INT)) {
                return elseExpr.asReferenceType().toUnboxedType().get();
            }
             
            /* Otherwise, binary numeric promotion (§5.6.2) is applied to the operand types,
             * and the type of the conditional expression is the promoted type of the second
             * and third operands.
             */
            ResolvedPrimitiveType PrimitiveThenExpr = thenExpr.isPrimitive() ? thenExpr.asPrimitive()
                    : thenExpr.asReferenceType().toUnboxedType().get();
            ResolvedPrimitiveType PrimitiveElseExpr = elseExpr.isPrimitive() ? elseExpr.asPrimitive()
                    : elseExpr.asReferenceType().toUnboxedType().get();
            return PrimitiveThenExpr.bnp(PrimitiveElseExpr);
        }
        
        /*
         * Otherwise, the conditional expression is a reference conditional expression.
         * A reference conditional expression is a poly expression if it appears in an assignment context or an
         * invocation context (§5.2. §5.3).
         * Otherwise, it is a standalone expression.
         * The type of a poly reference conditional expression is the same as its target type.
         * The type of a standalone reference conditional expression is determined as follows:
         * If the second and third operands have the same type (which may be the null type), then that is the type of
         * the conditional expression.
         * If the type of one of the second and third operands is the null type, and the type of the other operand is a
         * reference type, then the type of the conditional expression is that reference type.
         * Otherwise, the second and third operands are of types S1 and S2 respectively. Let T1 be the type that
         * results from applying boxing conversion to S1, and let T2 be the type that results from applying boxing
         * conversion to S2. The type of the conditional expression is the result of applying capture conversion
         * (§5.1.10) to lub(T1, T2).
         * TODO : must be implemented
         */
        if (node.isPolyExpression()) {
            // The type of a poly reference conditional expression is the same as its target type.
            Optional<Node> parentNode = node.getParentNode();
            if (parentNode.isPresent()) {
                Node parent = parentNode.get();
                if (parent instanceof AssignExpr) {
                    return visit((AssignExpr)parent, solveLambdas);
                } else if (parent instanceof MethodCallExpr) {
                    // how to define the target type?
                    // a priori it is the type of the parameter of the method which takes the value of the conditional expression
                    // TODO for the moment we keep the original return type
                    return thenExpr;
                }
                throw new RuntimeException("Cannot resolve type of poly expression "+ node.toString());
            } else {
                throw new RuntimeException("Parent node unexpectedly empty");
            }
            
        }
        
        // The type of a standalone reference conditional expression is determined as follows:
        
        // If the second and third operands have the same type (which may be the null type), then that is the type of
        // the conditional expression.
        if (thenExpr.equals(elseExpr)) {
            return thenExpr;
        }
        // If the type of one of the second and third operands is the null type, and the type of the other operand is a
        // reference type, then the type of the conditional expression is that reference type.
        // this case is already supported above
        
        // Otherwise, the second and third operands are of types S1 and S2 respectively. Let T1 be the type that
        // results from applying boxing conversion to S1, and let T2 be the type that results from applying boxing
        // conversion to S2. The type of the conditional expression is the result of applying capture conversion
        // (§5.1.10) to lub(T1, T2).
        ResolvedType resolvedThenType = thenExpr.isPrimitive() ? TypeHelper.toBoxedType(thenExpr.asPrimitive(), typeSolver) : thenExpr;
        ResolvedType resolvedElseType = elseExpr.isPrimitive() ? TypeHelper.toBoxedType(elseExpr.asPrimitive(), typeSolver) : elseExpr;
        
        // TypeHelper.leastUpperBound method is not yet implemented so for the moment we keep the original return type of this method
        // TODO implement TypeHelper.leastUpperBound method
        // return TypeHelper.leastUpperBound(new HashSet<ResolvedType>(Arrays.asList(resolvedThenType, resolvedElseType)));
        return node.getThenExpr().accept(this, solveLambdas);
    }
    
    private boolean isCompatible(ResolvedType resolvedType, ResolvedPrimitiveType primitiveType) {
        return (resolvedType.isPrimitive() && resolvedType.asPrimitive().equals(primitiveType))
        || (resolvedType.isReferenceType() && resolvedType.asReferenceType().isUnboxableTo(primitiveType));
    }

    @Override
    public ResolvedType visit(EnclosedExpr node, Boolean solveLambdas) {
        return node.getInner().accept(this, solveLambdas);
    }

    /**
     * Java Parser can't differentiate between packages, internal types, and fields.
     * All three are lumped together into FieldAccessExpr. We need to differentiate them.
     */
    private ResolvedType solveDotExpressionType(ResolvedReferenceTypeDeclaration parentType, FieldAccessExpr node) {
        // Fields and internal type declarations cannot have the same name.
        // Thus, these checks will always be mutually exclusive.
        if (parentType.isEnum() && parentType.asEnum().hasEnumConstant(node.getName().getId())) {
            return parentType.asEnum().getEnumConstant(node.getName().getId()).getType();
        } else if (parentType.hasField(node.getName().getId())) {
            return parentType.getField(node.getName().getId()).getType();
        } else if (parentType.hasInternalType(node.getName().getId())) {
            return new ReferenceTypeImpl(parentType.getInternalType(node.getName().getId()), typeSolver);
        } else {
            throw new UnsolvedSymbolException(node.getName().getId());
        }
    }

    @Override
    public ResolvedType visit(FieldAccessExpr node, Boolean solveLambdas) {
        // We should understand if this is a static access
        if (node.getScope() instanceof NameExpr ||
                node.getScope() instanceof FieldAccessExpr) {
            Expression staticValue = node.getScope();
            SymbolReference<ResolvedTypeDeclaration> typeAccessedStatically = JavaParserFactory.getContext(node, typeSolver).solveType(staticValue.toString());
            if (typeAccessedStatically.isSolved()) {
                // TODO here maybe we have to substitute type typeParametersValues
                return solveDotExpressionType(
                        typeAccessedStatically.getCorrespondingDeclaration().asReferenceType(), node);
            }
        } else if (node.getScope() instanceof ThisExpr) {
            // If we are accessing through a 'this' expression, first resolve the type
            // corresponding to 'this'
            SymbolReference<ResolvedTypeDeclaration> solve = facade.solve((ThisExpr) node.getScope());
            // If found get it's declaration and get the field in there
            if (solve.isSolved()) {
                ResolvedTypeDeclaration correspondingDeclaration = solve.getCorrespondingDeclaration();
                if (correspondingDeclaration instanceof ResolvedReferenceTypeDeclaration) {
                    return solveDotExpressionType(correspondingDeclaration.asReferenceType(), node);
                }
            }

        } else if (node.getScope().toString().indexOf('.') > 0) {
            // try to find fully qualified name
            SymbolReference<ResolvedReferenceTypeDeclaration> sr = typeSolver.tryToSolveType(node.getScope().toString());
            if (sr.isSolved()) {
                return solveDotExpressionType(sr.getCorrespondingDeclaration(), node);
            }
        }
        Optional<Value> value = Optional.empty();
        try {
            value = new SymbolSolver(typeSolver).solveSymbolAsValue(node.getName().getId(), node);
        } catch (UnsolvedSymbolException use) {
            // This node may have a package name as part of its fully qualified name.
            // We should solve for the type declaration inside this package.
            SymbolReference<ResolvedReferenceTypeDeclaration> sref = typeSolver.tryToSolveType(node.toString());
            if (sref.isSolved()) {
                return new ReferenceTypeImpl(sref.getCorrespondingDeclaration(), typeSolver);
            }
        }
        if (value.isPresent()) {
            return value.get().getType();
        }
        throw new UnsolvedSymbolException(node.getName().getId());
    }

    @Override
    public ResolvedType visit(InstanceOfExpr node, Boolean solveLambdas) {
        return ResolvedPrimitiveType.BOOLEAN;
    }

    @Override
    public ResolvedType visit(StringLiteralExpr node, Boolean solveLambdas) {
        return new ReferenceTypeImpl(new ReflectionTypeSolver().solveType(String.class.getCanonicalName()), typeSolver);
    }

    @Override
    public ResolvedType visit(IntegerLiteralExpr node, Boolean solveLambdas) {
        return ResolvedPrimitiveType.INT;
    }

    @Override
    public ResolvedType visit(LongLiteralExpr node, Boolean solveLambdas) {
        return ResolvedPrimitiveType.LONG;
    }

    @Override
    public ResolvedType visit(CharLiteralExpr node, Boolean solveLambdas) {
        return ResolvedPrimitiveType.CHAR;
    }

    @Override
    public ResolvedType visit(DoubleLiteralExpr node, Boolean solveLambdas) {
        if (node.getValue().toLowerCase().endsWith("f")) {
            return ResolvedPrimitiveType.FLOAT;
        }
        return ResolvedPrimitiveType.DOUBLE;
    }

    @Override
    public ResolvedType visit(BooleanLiteralExpr node, Boolean solveLambdas) {
        return ResolvedPrimitiveType.BOOLEAN;
    }

    @Override
    public ResolvedType visit(NullLiteralExpr node, Boolean solveLambdas) {
        return NullType.INSTANCE;
    }

    @Override
    public ResolvedType visit(MethodCallExpr node, Boolean solveLambdas) {
        Log.trace("getType on method call %s", ()-> node);
        // first solve the method
        MethodUsage ref = facade.solveMethodAsUsage(node);
        Log.trace("getType on method call %s resolved to %s", ()-> node, ()-> ref);
        Log.trace("getType on method call %s return type is %s", ()-> node, ref::returnType);
        return ref.returnType();
        // the type is the return type of the method
    }

    @Override
    public ResolvedType visit(NameExpr node, Boolean solveLambdas) {
        Log.trace("getType on name expr %s", ()-> node);
        Optional<Value> value = new SymbolSolver(typeSolver).solveSymbolAsValue(node.getName().getId(), node);
        if (!value.isPresent()) {
            throw new UnsolvedSymbolException("Solving " + node, node.getName().getId());
        } else {
            return value.get().getType();
        }
    }

    @Override
    public ResolvedType visit(TypeExpr node, Boolean solveLambdas) {
        Log.trace("getType on type expr %s", ()-> node);
        if (!(node.getType() instanceof ClassOrInterfaceType)) {
            // TODO / FIXME... e.g. System.out::println
            throw new UnsupportedOperationException(node.getType().getClass().getCanonicalName());
        }
        ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) node.getType();
        SymbolReference<ResolvedTypeDeclaration> typeDeclarationSymbolReference = JavaParserFactory
                .getContext(classOrInterfaceType, typeSolver)
                .solveType(classOrInterfaceType.getName().getId());
        if (!typeDeclarationSymbolReference.isSolved()) {
            throw new UnsolvedSymbolException("Solving " + node, classOrInterfaceType.getName().getId());
        } else {
            return new ReferenceTypeImpl(typeDeclarationSymbolReference.getCorrespondingDeclaration().asReferenceType(), typeSolver);
        }
    }

    @Override
    public ResolvedType visit(ObjectCreationExpr node, Boolean solveLambdas) {
        return facade.convertToUsage(node.getType(), node);
    }

    @Override
    public ResolvedType visit(ThisExpr node, Boolean solveLambdas) {
        // If 'this' is prefixed by a class eg. MyClass.this
        if (node.getTypeName().isPresent()) {
            // Get the class name
            String className = node.getTypeName().get().asString();
            // Attempt to resolve locally in Compilation unit
            // first try a buttom/up approach
            try {
                return new ReferenceTypeImpl(
                        facade.getTypeDeclaration(facade.findContainingTypeDeclOrObjectCreationExpr(node, className)),
                        typeSolver);
            } catch (IllegalStateException e) {
                // trying another approach from type solver
                Optional<CompilationUnit> cu = node.findAncestor(CompilationUnit.class);
                SymbolReference<ResolvedReferenceTypeDeclaration> clazz = typeSolver.tryToSolveType(className);
                if (clazz.isSolved()) {
                    return new ReferenceTypeImpl(clazz.getCorrespondingDeclaration(), typeSolver);
                }
            }
        }
        return new ReferenceTypeImpl(facade.getTypeDeclaration(facade.findContainingTypeDeclOrObjectCreationExpr(node)), typeSolver);
    }

    @Override
    public ResolvedType visit(SuperExpr node, Boolean solveLambdas) {
        // If 'super' is prefixed by a class eg. MyClass.this
        if (node.getTypeName().isPresent()) {
            String className = node.getTypeName().get().asString();
            SymbolReference<ResolvedTypeDeclaration> resolvedTypeNameRef = JavaParserFactory.getContext(node, typeSolver).solveType(className);
            if (resolvedTypeNameRef.isSolved()) {
                // Cfr JLS $15.12.1
                ResolvedTypeDeclaration resolvedTypeName = resolvedTypeNameRef.getCorrespondingDeclaration();
                if (resolvedTypeName.isInterface()) {
                    return new ReferenceTypeImpl(resolvedTypeName.asInterface(), typeSolver);
                } else if (resolvedTypeName.isClass()) {
                    // TODO: Maybe include a presence check? e.g. in the case of `java.lang.Object` there will be no superclass.
                    return resolvedTypeName.asClass().getSuperClass().orElseThrow(() -> new RuntimeException("super class unexpectedly empty"));
                } else {
                    throw new UnsupportedOperationException(node.getClass().getCanonicalName());
                }
            } else {
                throw new UnsolvedSymbolException(className);
            }
        }

        ResolvedTypeDeclaration typeOfNode = facade.getTypeDeclaration(facade.findContainingTypeDeclOrObjectCreationExpr(node));
        if (typeOfNode instanceof ResolvedClassDeclaration) {
            // TODO: Maybe include a presence check? e.g. in the case of `java.lang.Object` there will be no superclass.
            return ((ResolvedClassDeclaration) typeOfNode).getSuperClass().orElseThrow(() -> new RuntimeException("super class unexpectedly empty"));
        } else {
            throw new UnsupportedOperationException(node.getClass().getCanonicalName());
        }
    }

    @Override
    public ResolvedType visit(UnaryExpr node, Boolean solveLambdas) {
        switch (node.getOperator()) {
            case MINUS:
            case PLUS:
                return node.getExpression().accept(this, solveLambdas);
            case LOGICAL_COMPLEMENT:
                return ResolvedPrimitiveType.BOOLEAN;
            case POSTFIX_DECREMENT:
            case PREFIX_DECREMENT:
            case POSTFIX_INCREMENT:
            case PREFIX_INCREMENT:
            case BITWISE_COMPLEMENT:
                return node.getExpression().accept(this, solveLambdas);
            default:
                throw new UnsupportedOperationException(node.getOperator().name());
        }
    }

    @Override
    public ResolvedType visit(VariableDeclarationExpr node, Boolean solveLambdas) {
        if (node.getVariables().size() != 1) {
            throw new UnsupportedOperationException();
        }
        return facade.convertToUsageVariableType(node.getVariables().get(0));
    }


    @Override
    public ResolvedType visit(LambdaExpr node, Boolean solveLambdas) {
        if (demandParentNode(node) instanceof MethodCallExpr) {
            MethodCallExpr callExpr = (MethodCallExpr) demandParentNode(node);
            int pos = JavaParserSymbolDeclaration.getParamPos(node);
            SymbolReference<ResolvedMethodDeclaration> refMethod = facade.solve(callExpr);
            if (!refMethod.isSolved()) {
                throw new UnsolvedSymbolException(demandParentNode(node).toString(), callExpr.getName().getId());
            }
            Log.trace("getType on lambda expr %s", ()-> refMethod.getCorrespondingDeclaration().getName());
            if (solveLambdas) {

                // The type parameter referred here should be the java.util.stream.Stream.T
                ResolvedType result = refMethod.getCorrespondingDeclaration().getParam(pos).getType();

                if (callExpr.getScope().isPresent()) {
                    Expression scope = callExpr.getScope().get();

                    // If it is a static call we should not try to get the type of the scope
                    boolean staticCall = false;
                    if (scope instanceof NameExpr) {
                        NameExpr nameExpr = (NameExpr) scope;
                        try {
                            SymbolReference<ResolvedTypeDeclaration> type = JavaParserFactory.getContext(nameExpr, typeSolver).solveType(nameExpr.getName().getId());
                            if (type.isSolved()) {
                                staticCall = true;
                            }
                        } catch (Exception e) {

                        }
                    }

                    if (!staticCall) {
                        ResolvedType scopeType = facade.getType(scope);
                        if (scopeType.isReferenceType()) {
                            result = scopeType.asReferenceType().useThisTypeParametersOnTheGivenType(result);
                        }
                    }
                }

                // We need to replace the type variables
                Context ctx = JavaParserFactory.getContext(node, typeSolver);
                result = solveGenericTypes(result, ctx);

                //We should find out which is the functional method (e.g., apply) and replace the params of the
                //solveLambdas with it, to derive so the values. We should also consider the value returned by the
                //lambdas
                Optional<MethodUsage> functionalMethod = FunctionalInterfaceLogic.getFunctionalMethod(result);
                if (functionalMethod.isPresent()) {
                    LambdaExpr lambdaExpr = node;

                    InferenceContext lambdaCtx = new InferenceContext(MyObjectProvider.INSTANCE);
                    InferenceContext funcInterfaceCtx = new InferenceContext(MyObjectProvider.INSTANCE);

                    // At this point parameterType
                    // if Function<T=? super Stream.T, ? extends map.R>
                    // we should replace Stream.T
                    ResolvedType functionalInterfaceType = ReferenceTypeImpl.undeterminedParameters(functionalMethod.get().getDeclaration().declaringType(), typeSolver);

                    lambdaCtx.addPair(result, functionalInterfaceType);

                    ResolvedType actualType;

                    if (lambdaExpr.getBody() instanceof ExpressionStmt) {
                        actualType = facade.getType(((ExpressionStmt) lambdaExpr.getBody()).getExpression());
                    } else if (lambdaExpr.getBody() instanceof BlockStmt) {
                        BlockStmt blockStmt = (BlockStmt) lambdaExpr.getBody();

                        // Get all the return statements in the lambda block
                        List<ReturnStmt> returnStmts = blockStmt.findAll(ReturnStmt.class);

                        if (returnStmts.size() > 0) {
                            actualType = returnStmts.stream()
                                    .map(returnStmt -> returnStmt.getExpression().map(e -> facade.getType(e)).orElse(ResolvedVoidType.INSTANCE))
                                    .filter(x -> x != null && !x.isVoid() && !x.isNull())
                                    .findFirst()
                                    .orElse(ResolvedVoidType.INSTANCE);

                        } else {
                            actualType = ResolvedVoidType.INSTANCE;
                        }


                    } else {
                        throw new UnsupportedOperationException();
                    }

                    ResolvedType formalType = functionalMethod.get().returnType();

                    // Infer the functional interfaces' return vs actual type
                    funcInterfaceCtx.addPair(formalType, actualType);
                    // Substitute to obtain a new type
                    ResolvedType functionalTypeWithReturn = funcInterfaceCtx.resolve(funcInterfaceCtx.addSingle(functionalInterfaceType));

                    // if the functional method returns void anyway
                    // we don't need to bother inferring types
                    if (!(formalType instanceof ResolvedVoidType)) {
                        lambdaCtx.addPair(result, functionalTypeWithReturn);
                        result = lambdaCtx.resolve(lambdaCtx.addSingle(result));
                    }
                }

                return result;
            } else {
                return refMethod.getCorrespondingDeclaration().getParam(pos).getType();
            }
        } else {
            throw new UnsupportedOperationException("The type of a lambda expr depends on the position and its return value");
        }
    }

    @Override
    public ResolvedType visit(MethodReferenceExpr node, Boolean solveLambdas) {
        if (demandParentNode(node) instanceof MethodCallExpr) {
            MethodCallExpr callExpr = (MethodCallExpr) demandParentNode(node);
            int pos = JavaParserSymbolDeclaration.getParamPos(node);
            SymbolReference<ResolvedMethodDeclaration> refMethod = facade.solve(callExpr, false);
            if (!refMethod.isSolved()) {
                throw new UnsolvedSymbolException(demandParentNode(node).toString(), callExpr.getName().getId());
            }
            Log.trace("getType on method reference expr %s", ()-> refMethod.getCorrespondingDeclaration().getName());
            if (solveLambdas) {
                MethodUsage usage = facade.solveMethodAsUsage(callExpr);
                ResolvedType result = usage.getParamType(pos);
                // We need to replace the type variables
                Context ctx = JavaParserFactory.getContext(node, typeSolver);
                result = solveGenericTypes(result, ctx);

                //We should find out which is the functional method (e.g., apply) and replace the params of the
                //solveLambdas with it, to derive so the values. We should also consider the value returned by the
                //lambdas
                Optional<MethodUsage> functionalMethodOpt = FunctionalInterfaceLogic.getFunctionalMethod(result);
                if (functionalMethodOpt.isPresent()) {
                    MethodUsage functionalMethod = functionalMethodOpt.get();

                    for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> typeParamDecl : result.asReferenceType().getTypeParametersMap()) {
                        functionalMethod = functionalMethod.replaceTypeParameter(typeParamDecl.a, typeParamDecl.b);
                    }

                    // replace wildcards
                    for (int i = 0; i < functionalMethod.getNoParams(); i++) {
                        ResolvedType type = functionalMethod.getParamType(i);
                        if (type.isWildcard()) {
                            ResolvedType boundedType = type.asWildcard().getBoundedType();
                            functionalMethod = functionalMethod.replaceParamType(i, boundedType);
                        }
                    }

                    ResolvedType actualType = facade.toMethodUsage(node, functionalMethod.getParamTypes()).returnType();
                    ResolvedType formalType = functionalMethod.returnType();

                    InferenceContext inferenceContext = new InferenceContext(MyObjectProvider.INSTANCE);
                    inferenceContext.addPair(formalType, actualType);
                    result = inferenceContext.resolve(inferenceContext.addSingle(result));
                }

                return result;
            }
            return refMethod.getCorrespondingDeclaration().getParam(pos).getType();
        }
        throw new UnsupportedOperationException("The type of a method reference expr depends on the position and its return value");
    }

    @Override
    public ResolvedType visit(FieldDeclaration node, Boolean solveLambdas) {
        if (node.getVariables().size() == 1) {
            return node.getVariables().get(0).accept(this, solveLambdas);
        }
        throw new IllegalArgumentException("Cannot resolve the type of a field with multiple variable declarations. Pick one");
    }
}
