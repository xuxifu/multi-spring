package work;


import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.event.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.tools.jdi.SocketAttachingConnector;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static sun.management.jmxremote.ConnectorBootstrap.DefaultValues.PORT;
import static sun.util.locale.provider.LocaleProviderAdapter.Type.HOST;

public class AttachObjToJvm {

    public static int add(int a, int b){
        return  a + b;
    }

    private static void execute(Event event) throws Exception
    {
        if (event instanceof MethodEntryEvent)
        {
            System.out.println("Mehtod Entry:" + ((MethodEntryEvent) event).method());
        }
        else if (event instanceof MethodExitEvent)
        {
            System.out.println("Mehtod Exi:" + ((MethodExitEvent) event).method());
        }
    }

    public static void main(String[] args) throws Exception {
        //1、取得连接器
        VirtualMachineManager vmManager = Bootstrap.virtualMachineManager();
        List connectors = vmManager.attachingConnectors();
        AttachingConnector socketAttachingConnector = null;
        for (int i = 0; i < connectors.size(); i++)
        {
            Connector connector = (Connector) connectors.get(i);
            Transport transport = connector.transport();
            if ("dt_socket".equals(transport.name()))
            {
                socketAttachingConnector = (AttachingConnector) connector;
                break;
            }
        }
        //2、 连接到被调试的JVM上
        Map arguments = socketAttachingConnector.defaultArguments();
        Connector.Argument hostArg = (Connector.Argument) arguments.get(HOST);
        Connector.Argument portArg = (Connector.Argument) arguments.get(PORT);

        hostArg.setValue("127.0.0.1");
        portArg.setValue("8000");

        VirtualMachine jvm = socketAttachingConnector.attach(arguments);

        //3、打印当前线程信息
        List<ThreadReference> threadReferences = jvm.allThreads();
        for (ThreadReference tr : threadReferences)
        {
            System.out.print("Thread[" + tr.name() + "] : ");
            //线程状态
            switch (tr.status())
            {
                case ThreadReference.THREAD_STATUS_MONITOR:
                    System.out.println(" Waiting");
                    break;
                case ThreadReference.THREAD_STATUS_NOT_STARTED:
                    System.out.println(" Not Start");
                    break;
                case ThreadReference.THREAD_STATUS_RUNNING:
                    System.out.println(" Running");
                    break;
                case ThreadReference.THREAD_STATUS_SLEEPING:
                    System.out.println(" Sleepping");
                    break;
                case ThreadReference.THREAD_STATUS_UNKNOWN:
                    System.out.println(" Unknow");
                    break;
                case ThreadReference.THREAD_STATUS_WAIT:
                    System.out.println(" Wait");
                    break;
                case ThreadReference.THREAD_STATUS_ZOMBIE:
                    System.out.println(" Finish");
                    break;
            }
            boolean suspend = tr.isSuspended();
            //注意，只有suspend的线程才能获得其线程栈，因此需要将其线suspend一下
            if (!suspend)
            {
                tr.suspend();
            }
            List<StackFrame> frames = tr.frames();
            for (StackFrame frame : frames)
            {
                System.out.println("-----"
                        + frame.location().method().toString() + ":"
                        + frame.location().lineNumber());
            }

            System.out.println("count:" + tr.entryCount());

            if (!suspend)
            {
                tr.resume();
            }
            frames = null;

            /**
             * 4、注册方法进入和退出事件，在实现调试器的时候，
             * 我们可以通过注册一个BreakPoint事件，
             * 在事件发生时挂住执行线程，
             * 然后检查事件发生对象的信息来实现最常见的断点调试功能，
             * 或者Step事件来完成单步执行功能
             */

            EventRequestManager eventRequestManager = jvm.eventRequestManager();
            MethodEntryRequest methodEntryRequest = eventRequestManager.createMethodEntryRequest();
            methodEntryRequest.addClassExclusionFilter("java.*");//设置过滤器，对过滤器中的Class不捕获其实践
            methodEntryRequest.addClassExclusionFilter("sun.*");
            methodEntryRequest.addClassExclusionFilter("javax.*");
            methodEntryRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);//这个属性一定要设，
            //默认事件发生时会suspend住执行线程
            methodEntryRequest.enable();

            MethodExitRequest methodExitRequest = eventRequestManager.createMethodExitRequest();
            methodExitRequest.addClassExclusionFilter("java.*");
            methodExitRequest.addClassExclusionFilter("sun.*");
            methodExitRequest.addClassExclusionFilter("javax.*");
            methodExitRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            methodExitRequest.enable();

            EventQueue eventQueue = jvm.eventQueue();
            EventSet eventSet;
            while (true)
            {
                eventSet = eventQueue.remove();
                EventIterator eventIterator = eventSet.eventIterator();
                while (eventIterator.hasNext())
                {
                    Event event = (Event) eventIterator.next();
                    execute(event);
                }
            }
        }
    }
}
