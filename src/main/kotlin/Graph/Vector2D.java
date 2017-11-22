package Graph;

public class Vector2D {
    public final double x;
    public final double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public Vector2D plus(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }
    public Vector2D minus(Vector2D other) {
        return new Vector2D(this.x - other.x, this.y - other.y);
    }
    public double distance(Vector2D other) {
        Vector2D diff = this.minus(other);
        return Math.sqrt(diff.x * diff.x + diff.y * diff.y);
    }
}
