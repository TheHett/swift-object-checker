import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.NotFoundException;
import org.javaswift.joss.model.Account;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class ObjectCheckerService {

    private final static Logger logger = LoggerFactory.getLogger(ObjectCheckerService.class);
    private int concurrency = 40;
    final ExecutorService threadPool = Executors.newFixedThreadPool(concurrency);

    public ObjectCheckerService() {
    }

    public ObjectCheckerService(int concurrency) {
        this.concurrency = concurrency;
    }

    public void check(Container container, Report report) {
        logger.debug("Check container {}", container.getName());
        report.incContainersCheckedCount();
        try {
            for (StoredObject object : container.list()) {
                threadPool.submit(() -> {
                    logger.debug("Checking object: {}", object.getName());
                    try {
                        // this method checks really object existent,
                        // not just in the container listing
                        if (object.exists()) {
                            report.incSuccessObjectsCount();
                            logger.debug("Success object check: {}", object.getName());
                        } else {
                            report.incNotFoundObjectsCount();
                            report.getNotFoundObjects().add(container.getName() + "/" + object.getName());
                            logger.warn("Object not found: {}", object.getName());
                        }
                    } catch (CommandException e) {
                        report.incFailedObjectsCount();
                        logger.error("Error check object: {}", e.getMessage());
                    }
                    report.incObjectsCheckedCount();
                });
            }
        } catch (CommandException e) {
            // the error occurred when trying to get container listing
            logger.warn("Error check container {}", container.getName(), e);
        }
    }

    public void shutdown() throws InterruptedException {
        threadPool.shutdown();
        threadPool.awaitTermination(5, TimeUnit.SECONDS);
    }

    public Report checkAllContainers(Account account) {
        final var totalReport = new Report();
        final var pageSize = 100;
        logger.info("Preparing pagination map...");
        var paginationMap = account.getPaginationMap(pageSize);

        logger.info("Start checking");
        for (int page = 0; page < paginationMap.getNumberOfPages(); page++) {
            for (Container container : account.list(paginationMap, page)) {
                this.check(container, totalReport);
            }
            logger.info(totalReport.asString());
        }
        return totalReport;
    }
}
