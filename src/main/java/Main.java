import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;


public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    PrintWriter writer = new PrintWriter("work/foobar.txt", "UTF-8");
    writer.println("The first line");
    writer.close();

    resp.getWriter().print("Hello from Java!");
  }

  public static void main(String[] args) throws Exception{
    final WatchService watchService = FileSystems.getDefault().newWatchService();
    Path path = Paths.get(args.length > 0 ? args[0] : "/etc/heroku/space-topology.json");
    path.register(watchService,ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("Goodbye world");
        try {
          watchService.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();

    while (true) {
      try {
        WatchKey key = watchService.take();
        if (key != null) {
          for (WatchEvent<?> event : key.pollEvents()) {
            System.out.println("Received watch event: " + event.context().toString());
          }
          boolean reset = key.reset();
          if (!reset) {
            System.out.println("Could not reset the watch key.");
            break;
          }
        }
      } catch (ClosedWatchServiceException e) {
        // do nothing
      }
    }
  }
}
