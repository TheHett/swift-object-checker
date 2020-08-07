import org.apache.log4j.Logger;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.NotFoundException;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;


import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class ObjectCheckerService {

    private final static Logger logger = Logger.getLogger(ObjectCheckerService.class);
    private int concurrency = 40;

    final ExecutorService pool = Executors.newFixedThreadPool(concurrency);

    public Report check(Container container, Consumer<StoredObject> onNotFound) throws InterruptedException {
        final var report = new Report();
        logger.debug("Checking container: " + container.getName());
        report.incContainersCheckedCount();
        var callable = new ArrayList<Callable<Object>>(container.getCount());

        container.list().forEach((final StoredObject object) -> {
            logger.debug("Checking object: " + object.getName());
            callable.add(Executors.callable(() -> {
                report.incObjectsCheckedCount();
                try {
                    var md5 = object.getEtag();
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
            }));
        });
        pool.invokeAll(callable);
        return report;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public void shutdown() {
        pool.shutdown();
    }

}
