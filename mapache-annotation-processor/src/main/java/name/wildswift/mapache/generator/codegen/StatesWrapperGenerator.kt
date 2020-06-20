package name.wildswift.mapache.generator.codegen

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.squareup.javapoet.*
import name.wildswift.mapache.generator.*
import name.wildswift.mapache.generator.codegen.GenerationConstants.createInstanceMethodName
import name.wildswift.mapache.generator.codegen.GenerationConstants.getWrappedMethodName
import name.wildswift.mapache.generator.generatemodel.StateDefinition
import name.wildswift.mapache.generator.parsers.groovydsl.Action
import name.wildswift.mapache.generator.parsers.groovydsl.State
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier


class StatesWrapperGenerator(
        private val packageName: String,
        private val baseTypeName: ClassName,
        private val actionBaseType: ClassName,
        private val processingEnv: ProcessingEnvironment,
        private val moduleBuildConfig: ClassName,
        private val dependencySource: TypeName,
        private val states: List<StateDefinition>
) {

    val stateNames = states.map { state -> state.name to ClassName.bestGuess(state.name) }.toMap()


    private val navigationContextType = ParameterizedTypeName.get(navigationContextTypeName, actionBaseType, dependencySource)
    private val navigationContextParameter = ParameterSpec.builder(navigationContextType, "context").addAnnotation(NonNull::class.java).build()

    @SuppressWarnings("DefaultLocale")
    fun generateAll() {
        val rootTypeVariable = TypeVariableName.get("VR", viewClass)
        val wrappedTypeVarible = TypeVariableName.get("MS", ParameterizedTypeName.get(mStateTypeName, actionBaseType, genericWildcard, rootTypeVariable, dependencySource))
        val getWrappedMethod = MethodSpec.methodBuilder(getWrappedMethodName)
                .addAnnotation(NonNull::class.java)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(wrappedTypeVarible)
                .build()

        val baseClassTypeSpec = TypeSpec
                .classBuilder(baseTypeName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addTypeVariable(rootTypeVariable)
                .addTypeVariable(wrappedTypeVarible)
                .addSuperinterface(ParameterizedTypeName.get(mStateTypeName, actionBaseType, viewSetTypeName, rootTypeVariable, dependencySource))
                .addSuperinterface(ParameterizedTypeName.get(navigatableTypeName, actionBaseType, dependencySource, ParameterizedTypeName.get(baseTypeName, rootTypeVariable, genericWildcard)))
                .addMethod(getWrappedMethod)
                .build()

        processingEnv.filer.createSourceFile(baseTypeName.canonicalName())
                .openWriter()
                .use { fileWriter ->
                    JavaFile.builder(packageName, baseClassTypeSpec)
                            .build()
                            .writeTo(fileWriter)
                }

        states.forEach { state ->

            val stateRootViewType = viewGroupClass

            println(state.name)
            val thisStateViewSetType = processingEnv.elementUtils.getTypeElement(state.name).extractViewSetType()

            val currentStateWrapperName = state.wrapperClassName
            val currentStateName = stateNames[state.name] ?: error("Internal error")

            val wrappedField = FieldSpec.builder(currentStateName, "wrappedObj").addModifiers(Modifier.PRIVATE, Modifier.FINAL).build()
            val parameterList = state.parameters.orEmpty()

            val stateWrapperTypeSpecBuilder = TypeSpec
                    .classBuilder(currentStateWrapperName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .superclass(ParameterizedTypeName.get(baseTypeName, stateRootViewType, currentStateName))
                    .addField(wrappedField)
                    /*
                      @Override
                      @NonNull
                      public BuyStep1State getWrapped() {
                        return wrappedObj;
                      }
                     */
                    .addMethod(MethodSpec.methodBuilder(getWrappedMethodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override::class.java)
                            .addAnnotation(NonNull::class.java)
                            .returns(currentStateName)
                            .addStatement("return \$N", wrappedField)
                            .build()
                    )
                    /*
                      @Override
                      @Nullable
                      public TestAppMState getNextState(@NonNull TestAppEvent e) {
                        if (e instanceof ProceedBuy) return ReviewBuyStateWrapper.newInstance(((ProceedBuy)e).getTiker(), ((ProceedBuy)e).getAmount(), ((ProceedBuy)e).getPaymentType());
                        if (BuildConfig.DEBUG) throw new IllegalStateException("Unable to process event " + e.getClass().getSimpleName());
                        return null;
                      }
                     */
                    .addMethod(MethodSpec.methodBuilder("getNextState")
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override::class.java)
                            .addAnnotation(Nullable::class.java)
                            .addParameter(ParameterSpec.builder(actionBaseType, "e").addAnnotation(NonNull::class.java).build())
                            .returns(baseTypeName)
                            .apply {
                                state.moveDefenition.forEach { moveDefenition ->
                                    val parametersSting = moveDefenition.moveParameters.joinToString { "((\$1T)e).get${it.name.capitalize()}()" }
                                    addStatement("if (e instanceof \$1T) return \$2T.${createInstanceMethodName}($parametersSting)", moveDefenition.actionType, moveDefenition.targetStateWrapperClass)
                                }
                                addStatement("if (\$1T.DEBUG) throw new \$2T(\"Unable to process event \" + e.getClass().getSimpleName())", moduleBuildConfig, ClassName.get(IllegalStateException::class.java))
                                addStatement("return null")
                            }
                            .build()
                    )
                    /*
                      @Override
                      @NonNull
                      public ViewCouple<RootView, BuyCurrencyStep1View> setup(@NonNull ViewGroup rootView, @NonNull NavigationContext<TestAppEvent, DiContext> context) {
                        return wrappedObj.setup(rootView, context);
                      }
                     */
                    .addMethod(MethodSpec.methodBuilder("setup")
                            .addAnnotation(Override::class.java)
                            .addAnnotation(NonNull::class.java)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(ParameterSpec.builder(viewGroupClass, "rootView").addAnnotation(NonNull::class.java).build())
                            .addParameter(navigationContextParameter)
                            .returns(thisStateViewSetType)
                            .addStatement("return \$N.setup(rootView, context)", wrappedField)
                            .build()
                    )
                    /*
                      @Override
                      @NonNull
                      @SuppressWarnings("unchecked")
                      public Runnable dataBind(@NonNull NavigationContext<TestAppEvent, DiContext> context, @NonNull ViewSet views) {
                        return wrappedObj.dataBind(context, (ViewCouple<RootView, BuyCurrencyStep1View>) views);
                      }
                     */
                    .addMethod(MethodSpec.methodBuilder("dataBind")
                            .addAnnotation(Override::class.java)
                            .addAnnotation(NonNull::class.java)
                            .addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java).addMember("value", "\"unchecked\"").build())
                            .addModifiers(Modifier.PUBLIC)
                            .returns(runnableTypeName)
                            .addParameter(navigationContextParameter)
                            .addParameter(ParameterSpec.builder(viewSetTypeName, "views").addAnnotation(NonNull::class.java).build())
                            .addStatement("return \$N.dataBind(context, (\$T) views)", wrappedField, thisStateViewSetType)
                            .build()
                    )
                    /*
                      @Override
                      @NonNull
                      public Runnable start(@NonNull NavigationContext<TestAppEvent, DiContext> context) {
                        return wrappedObj.start(context);
                      }
                     */
                    .addMethod(MethodSpec.methodBuilder("start")
                            .addAnnotation(Override::class.java)
                            .addAnnotation(NonNull::class.java)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(runnableTypeName)
                            .addParameter(navigationContextParameter)
                            .addStatement("return \$N.start(context)", wrappedField)
                            .build()
                    )

            if (parameterList.isEmpty()) {
                val instanceField = FieldSpec.builder(currentStateWrapperName, "instance", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).initializer("new \$T()", currentStateWrapperName).build()
                stateWrapperTypeSpecBuilder
                        .addField(instanceField)
                        .addMethod(MethodSpec.methodBuilder(createInstanceMethodName)
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(currentStateWrapperName)
                                .addStatement("return \$N", instanceField)
                                .build())
                        .addMethod(MethodSpec
                                .constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addStatement("\$N = new \$T()", wrappedField, currentStateName)
                                .build()
                        )
            } else {
                stateWrapperTypeSpecBuilder
                        .addMethod(MethodSpec.methodBuilder(createInstanceMethodName)
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .addParameters(
                                        parameterList.map {
                                            ParameterSpec.builder(it.type, it.name).build()
                                        }
                                )
                                .returns(currentStateWrapperName)
                                .addStatement("return new \$T(${parameterList.joinToString { it.name }})", currentStateWrapperName)
                                .build())
                        .addMethod(MethodSpec
                                .constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .addParameters(
                                        parameterList.map {
                                            ParameterSpec.builder(it.type, it.name).build()
                                        }
                                )
                                .addStatement("\$N = new \$T(${parameterList.joinToString { it.name }})", wrappedField, currentStateName)
                                .build()
                        )
            }

            processingEnv.filer.createSourceFile(currentStateWrapperName.canonicalName())
                    .openWriter()
                    .use { fileWriter ->
                        JavaFile.builder(packageName, stateWrapperTypeSpecBuilder.build())
                                .build()
                                .writeTo(fileWriter)
                    }
        }
    }

}