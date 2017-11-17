package work;

public class AttachTest {

    public static void main(String[] args) {
        new Thread() {
            @Override
            public void run() {
                AttachTest test = new AttachTest();
                while (true) {
                    try {
                        sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    test.sayHello();
                }
            }
        }.start();
    }

    protected void sayHello() {
        System.out.println("正在执行sayHello()方法");

    }

}
