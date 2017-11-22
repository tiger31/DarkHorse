package Graph;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Graph<T> {
    public Map<T, Vertex<T>> vertexes = new HashMap<>();

    public void addVertex(Vertex<T> vertex) {
        vertexes.put(vertex.value, vertex);
    }
    public void addVertexes(Collection<Vertex<T>> list) {
       for (Vertex<T> v : list)
           vertexes.put(v.value, v);
    }
    //public Vertex<T> get(int index) {
        //return vertexes.get(index);
    //}
    public Vertex<T> get(T value) {
        return vertexes.get(value);
    }

    public List<Vertex<T>> widthSearch(Vertex<T> from, Vertex<T> to) {
        //Сбрасываем информацию вершин
        clear();
        //Очередь вершин, которые мы должны пройти
        Deque<Vertex<T>> queue = new ArrayDeque<Vertex<T>>();
        //Добавляем первую вершину, чтобы начать поиск
        queue.add(from);
        //Продолжаем поиск по графу, пока все вершины в очереди не закончатся
        //Если закончились и путь мы не нашли, значит из начальной вершины - конечная недостижима
        while(!queue.isEmpty()) {
            //Берем первую из добавленных вершин и удаляем ее из очереди
            Vertex<T> current = queue.poll();
            //Продолжаем с ней работать, если мы ее не посещали
            if (!current.visited) {
                //Если эта вершина - конечная
                if (current.value == to.value) {
                    return current.getPath();
                }
                //Если вершина не та, которую нужно найти - добавляем всех ее "соседей" в очередь
                current.links.forEach(link -> {
                    //Непосредственно добавление в очередь
                    queue.add(link.end);
                    //Считаем путь до нее = путь до текущий + вес ребра до "соседа"
                    link.end.distance = current.distance + link.value;
                    //Сохраняем предыдущую, для итогового поиска пути
                    link.end.prev = current;
                });
                //Добавляем пройденную вершину в посещенные, чтобы больше не "прыгать" на нее
                current.visited = false;
            }
        }
        //Если вершина недостижима - возвращаем null
        return null;
    }
    public List<Vertex<T>> deepSearch(Vertex<T> from, Vertex<T> to) {
        if (!from.links.isEmpty()) {
            from.prev = null;
            from.distance = 0;
            from.visited = true;
            return deepSearch((Edge)from.links.get(0), to, 0);
        } else return null;
    }

    @Nullable
    public List<Edge<T>> aStar(Vertex<T> from, Vertex<T> to) {
        clear();
        PriorityQueue<Vertex> queue = new PriorityQueue<>(10,
                (o1, o2) -> {
                    double d1 = o1.position.distance(to.position);
                    double d2 = o2.position.distance(to.position);
                    return (d1 < d2) ? -1 : (d1 > d2) ? 1 : 0;
                });
        queue.offer(from);
        while(!queue.isEmpty()) {
            Vertex<T> current = queue.poll();
            if (!current.visited) {
                if (current.value.equals(to.value)) {
                    return current.getEdgePath();
                }
                current.links.forEach(link -> {
                    queue.offer(link.end);
                    link.end.distance = current.distance + link.value;
                    if (!link.end.visited)
                        link.end.prev = current;
                });
                current.visited = true;
            }
        }
        return null;
    }

    private List<Vertex<T>> deepSearch(Edge edge, Vertex<T> to, int depth) {
        Vertex<T> current = edge.end;
        if (!current.visited) {
            current.visited = true;
            current.prev = edge.start;
            current.distance = edge.start.distance + edge.value;
            if (current.value == to.value) {
                return current.getPath();
            } else {
                for (Object link : current.links) {
                    Edge e = (Edge) link;
                    List<Vertex<T>> path = deepSearch(e, to, depth + 1);
                    if (path != null) return path;
                }
                return null;
            }
        }
        return null;
    }

    private void clear(){
        vertexes.forEach((key, vertex) -> {
            vertex.visited = false;
            vertex.prev = null;
            vertex.distance = Integer.MAX_VALUE;
        });
    }
}

