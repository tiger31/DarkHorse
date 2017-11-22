package Graph;

import ru.spbstu.competition.protocol.data.River;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Vertex<T> {
    public final T value;
    public final Vector2D position;
    public boolean is_mine;
    public List<Edge<T>> links = new LinkedList<>();

    public Vertex<T> prev;
    public int distance = Integer.MAX_VALUE;
    public boolean visited = false;


    public Vertex(T value) {
        this.value = value;
        position = null;
        is_mine =  false;
    }
    public Vertex(T value, Vector2D pos) {
        this.value = value;
        this.position = pos;
    }
    public Vertex(T value, double x, double y) {
        this.value = value;
        this.position = new Vector2D(x, y);
    }

    public void link(Vertex<T> to) {
        this.links.add(new Edge<T>(this, to));
    }
    public void link(Vertex<T> to, int value) {
        this.links.add(new Edge<T>(this, to, value));
    }
    public void linkBoth(Vertex<T> to, River river) {
        this.links.add(new Edge<T>(this, to, river));
        to.links.add(new Edge<T>(to, this, river));
    }
    public void linkBoth(Vertex<T> to, int value) {
        this.links.add(new Edge<T>(this, to, value));
        to.links.add(new Edge<T>(to, this, value));
    }

    public List<Vertex<T>> getPath() {
        List<Vertex<T>> path = new ArrayList<Vertex<T>>();
        //Добавляем найденную вершину в список пройденных
        path.add(this);
        //Берем ее предыдущую
        Vertex<T> previous = this.prev;
        //Добавляем вершины пока предыдущая не будет равна null, т.е. пока мы не упремся в начальную вершину
        while (previous != null) {
            //Добавляем вершину
            path.add(previous);
            //Прыгаем на предыдущую
            previous = previous.prev;
        }
        //Переворачиваем лист, потому что мы добавляли вершины от конца к начальной
        Collections.reverse(path);
        //
        return path;
    }
    public List<Edge<T>> getEdgePath() {
        List<Edge<T>> path = new ArrayList<Edge<T>>();
        Vertex<T> current = this;
        Vertex<T> previous = this.prev;
        while (previous != null) {
            for (Edge<T> e : previous.links) {
                if (e.end == current && e.start == previous) {
                    path.add(e);
                    current = previous;
                    previous = previous.prev;
                    break;
                }
            }
        }
        return path;
    }
}
