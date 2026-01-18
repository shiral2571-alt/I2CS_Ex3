package assignments.Ex3;

public class Ex3ServerMain {
    public static void main(String[] args) {
        MyPacmanServer g = new MyPacmanServer();
        g.init(0, "default", false, 60, 120.0, 20, 15);
        g.play();
    }
}
