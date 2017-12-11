package Graph;

import org.jetbrains.annotations.Nullable;
import ru.spbstu.competition.game.RiverState;
import ru.spbstu.competition.protocol.data.River;

import java.util.*;

public class Graph<T> {
    public Map<T, Vertex<T>> vertexes = new HashMap<>();
    public Map<River, RiverState> riverStateMap;

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
        PriorityQueue<Vertex<T>> queue = new PriorityQueue<>(10, getComparator(to));
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

    private Comparator<Vertex<T>> getComparator(Vertex<T> to) {
        return (o1, o2) -> {
            double d1 = o1.position.distance(to.position);
            double d2 = o2.position.distance(to.position);
            return (d1 < d2) ? -1 : (d1 > d2) ? 1 : 0;
        };
    }

    @Nullable
    public List<Edge<T>> bidirectionalSearch(Vertex<T> from, Vertex<T> to) {
        clear();
        PriorityQueue<Vertex<T>> firstDirQueue = new PriorityQueue<Vertex<T>>(10, getComparator(to));
        PriorityQueue<Vertex<T>> secondDirQueue = new PriorityQueue<Vertex<T>>(10, getComparator(from));

        List<Vertex<T>> firstDirVisited = new ArrayList<Vertex<T>>();
        List<Vertex<T>> secondDirVisited = new ArrayList<Vertex<T>>();

        firstDirQueue.offer(from);
        secondDirQueue.offer(to);

        while (!firstDirQueue.isEmpty() && !secondDirQueue.isEmpty()) {
            //В первом направлении
            Vertex<T> first = firstDirQueue.poll();
            if (!firstDirVisited.contains(first)) {
                if (first == to) {
                    return first.getEdgePath();
                }
                for (Edge<T> link : first.links) {
                    if (secondDirVisited.contains(link.end) || secondDirQueue.contains(link.end)) {
                        List<Edge<T>> path = new ArrayList<Edge<T>>();
                        path.addAll(first.getEdgePath());
                        path.add(link);
                        List<Edge<T>> part = link.end.getEdgePath();
                        Collections.reverse(part);
                        path.addAll(part);
                        return path;
                    } else {
                        if (riverStateMap.get(link.river) != RiverState.Enemy) {
                            firstDirQueue.offer(link.end);
                            link.end.distance = first.distance + link.value;
                            if (!link.end.visited)
                                link.end.prev = first;
                        }
                    }
                }
                firstDirVisited.add(first);
                first.visited = true;
            }

            Vertex<T> second = secondDirQueue.poll();
            if (!secondDirVisited.contains(second)) {
                if (second == from) {
                    return second.getEdgePath();
                }
                for (Edge<T> link : second.links) {
                    if (firstDirVisited.contains(link.end) || firstDirQueue.contains(link.end)) {
                        List<Edge<T>> path = new ArrayList<Edge<T>>();
                        path.addAll(link.end.getEdgePath());
                        List<Edge<T>> part = second.getEdgePath();
                        part.add(link);
                        Collections.reverse(part);
                        path.addAll(part);
                        return path;
                    } else {
                        if (riverStateMap.get(link.river) != RiverState.Enemy) {
                            secondDirQueue.offer(link.end);
                            link.end.distance = second.distance + link.value;
                            if (!link.end.visited)
                                link.end.prev = second;
                        }
                    }
                }
                secondDirVisited.add(second);
                second.visited = true;
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

