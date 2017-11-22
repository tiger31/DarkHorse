package Graph;

import ru.spbstu.competition.protocol.data.River;

public class Edge<T> {
    public final Vertex<T> start;
    public final Vertex<T> end;
    public final int value;
    public final River river;

    Edge(Vertex<T> start, Vertex<T> end, int value) {
        this.start = start;
        this.end = end;
        this.river = null;
        this.value = value;
    }

    Edge(Vertex<T> start, Vertex<T> end) {
        this.start = start;
        this.end = end;
        this.river = null;
        this.value = 1;
    }
    Edge(Vertex<T> start, Vertex<T> end, River river) {
        this.start = start;
        this.end = end;
        this.river = river;
        this.value = 1;
    }
}
