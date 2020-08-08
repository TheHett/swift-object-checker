import org.apache.log4j.Logger;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.NotFoundException;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class ObjectCheckerService {

    private final static Logger logger = Logger.getLogger(ObjectCheckerService.class);
    private int concurrency = 40;
    final ExecutorService threadPool = Executors.newFixedThreadPool(concurrency);

    public ObjectCheckerService() {
    }

    public ObjectCheckerService(int concurrency) {
        this.concurrency = concurrency;
    }

    public void check(Container container, Report report, Consumer<StoredObject> onNotFound) {
        report.incContainersCheckedCount();
        try {
            for (StoredObject object : container.list()) {
                threadPool.submit(() -> {
                    logger.debug("Checking object: " + object.getName());
                    try {
                        // we doing any request to swift object and see if this led to exception
                        object.getEtag();
                        report.incSuccessObjectsCount();
                        logger.debug("Success object check: " + object.getName());
                    } catch (NotFoundException e) {
                        report.incNotFoundObjectsCount();
                        report.getNotFoundObjects().add(object);
                        onNotFound.accept(object);
                        logger.warn("Object not found: " + object.getName());
                    } catch (CommandException e) {
                        report.incFailedObjectsCount();
                        logger.error("Error check object: " + e.getMessage());
                    }
                    report.incObjectsCheckedCount();
                });
            }
        } catch (CommandException e) {
            // the error occurred when trying to get container listing
            logger.warn("Error check container " + container.getName(), e);
        }
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    public Report checkAllContainers(Account account, File log) throws IOException, InterruptedException {
        final var totalReport = new Report();
        try (var writer = new PrintWriter(log, StandardCharsets.UTF_8)) {
            final var pageSize = 100;
            var paginationMap = account.getPaginationMap(pageSize);
            for (int page = 0; page < paginationMap.getNumberOfPages(); page++) {
                for (Container container : account.list(paginationMap, page)) {
                    this.check(container, totalReport, (object) -> {
                        synchronized (Application.class) {
                            writer.println(container.getName() + "/" + object.getName());
                        }
                    });
                }
                logger.info(totalReport.asString());
            }
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        }
        return totalReport;
    }
}
