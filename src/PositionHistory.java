public class PositionHistory {
    public int positionHash;
    public int count;
    public PositionHistory parent;

    public PositionHistory(int positionHash, PositionHistory parent) {
        this.positionHash = positionHash;
        this.parent = parent;
        this.count = 1;
        PositionHistory tmp = this.parent;
        while (tmp.parent != null && this.count < 3) {
            if (tmp.positionHash == positionHash) this.count++;
            tmp = tmp.parent;
        }
    }

    public PositionHistory(int positionHash) {
        this.positionHash = positionHash;
        this.count = 1;
    }
}
