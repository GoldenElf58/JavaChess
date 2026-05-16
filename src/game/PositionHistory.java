package game;

public class PositionHistory {
    public long positionHash;
    public int count;
    public PositionHistory parent;

    public PositionHistory init(long positionHash, PositionHistory parent) {
        this.positionHash = positionHash;
        this.parent = parent;
        this.count = 1;
        PositionHistory tmp = this.parent;
        while (tmp.parent != null && this.count < 3) {
            if (tmp.positionHash == positionHash) this.count++;
            tmp = tmp.parent;
        }
        return this;
    }

    public PositionHistory init(long positionHash) {
        this.positionHash = positionHash;
        this.count = 1;
        this.parent = null;
        return this;
    }

    public PositionHistory(long positionHash, PositionHistory parent) {
        init(positionHash, parent);
    }

    public PositionHistory(long positionHash) {
        this.positionHash = positionHash;
        this.count = 1;
    }
}
