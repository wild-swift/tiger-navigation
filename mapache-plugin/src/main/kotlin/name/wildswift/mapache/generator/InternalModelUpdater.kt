package name.wildswift.mapache.generator

import name.wildswift.mapache.generator.dslmodel.Action
import name.wildswift.mapache.generator.dslmodel.StateMachine
import name.wildswift.mapache.generator.dslmodel.StateMachineLayer

object InternalModelUpdater {

    fun verifyAndUpdateInternalModel(stateMachine: StateMachine) {

        stateMachine.layers.forEachIndexed { index, layer ->
            verifyAndUpdateLayer(layer, null)
        }

    }

    private fun verifyAndUpdateLayer(stateMachine: StateMachineLayer, backedAction: Action?) {

    }
}