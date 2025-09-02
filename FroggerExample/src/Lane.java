class Lane {
    final int y;          // top y of the lane
    final double speed;   // +right, -left
    final int spawnEvery; // ticks between spawns
    Lane(int y, double speed, int spawnEvery) {
        this.y = y; this.speed = speed; this.spawnEvery = spawnEvery;
    }
}
