package name.wildswift.mapache.graph;

import android.util.Pair;

import androidx.annotation.NonNull;

import name.wildswift.mapache.events.Event;
import name.wildswift.mapache.states.MState;
import name.wildswift.mapache.viewsets.ViewSet;

public interface NavigationGraph<E extends Event, DC, NS extends MState<E, ? extends ViewSet, DC>> {
    // TODO think about specify view sets
    Pair<NS, StateTransition<E, ViewSet, ViewSet, DC>>  getNextState(@NonNull NS currentState, @NonNull E e);
}