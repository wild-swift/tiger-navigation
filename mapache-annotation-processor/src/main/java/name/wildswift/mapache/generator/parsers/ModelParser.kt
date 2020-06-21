package name.wildswift.mapache.generator.parsers

import name.wildswift.mapache.config.ConfigType
import name.wildswift.mapache.generator.generatemodel.GenerateModel
import name.wildswift.mapache.generator.parsers.groovy.StateMachine
import name.wildswift.mapache.generator.parsers.groovy.GroovyDslParser
import java.io.File
import javax.annotation.processing.ProcessingEnvironment

interface ModelParser {

    fun getModel(file: File, prefix:String, modulePackageName: String, processingEnv: ProcessingEnvironment) : GenerateModel

    companion object {
        fun getInstance(type: ConfigType) : ModelParser {
            when(type){
                ConfigType.GROOVY -> return GroovyDslParser()
                ConfigType.XML -> return XmlParser()
            }

        }
    }
}