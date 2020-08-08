import org.javaswift.joss.model.StoredObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public class Report {

    private final AtomicLong containersCheckedCount = new AtomicLong();
    private final AtomicLong objectsCheckedCount = new AtomicLong();
    private final AtomicLong successObjectsCount = new AtomicLong();
    private final AtomicLong failedCheckObjectsCount = new AtomicLong();
    private final AtomicLong notFoundObjectsCount = new AtomicLong();
    private final Collection<String> notFoundObjects = Collections.synchronizedList(new ArrayList<>());

    public long getContainersCheckedCount() {
        return containersCheckedCount.get();
    }

    public void incContainersCheckedCount() {
        this.containersCheckedCount.getAndIncrement();
    }

    public long getObjectsCheckedCount() {
        return objectsCheckedCount.get();
    }

    public void incObjectsCheckedCount() {
        this.objectsCheckedCount.getAndIncrement();
    }

    public long getSuccessObjectsCount() {
        return successObjectsCount.get();
    }

    public void incSuccessObjectsCount() {
        this.successObjectsCount.getAndIncrement();
    }

    public long getFailedCheckObjectsCount() {
        return failedCheckObjectsCount.get();
    }

    public void incFailedObjectsCount() {
        this.failedCheckObjectsCount.getAndIncrement();
    }

    public long getNotFoundObjectsCount() {
        return notFoundObjectsCount.get();
    }

    public void incNotFoundObjectsCount() {
        this.notFoundObjectsCount.getAndIncrement();
    }

    public Collection<String> getNotFoundObjects() {
        return notFoundObjects;
    }

    public String asString() {
        return String.format(
                "containers checked %s, objects checked %s [success %s, not found %s, failed %s]",
                containersCheckedCount,
                objectsCheckedCount,
                successObjectsCount,
                notFoundObjectsCount,
                failedCheckObjectsCount
        );
    }

    public void appendFrom(Report report) {
        objectsCheckedCount.addAndGet(report.getObjectsCheckedCount());
        successObjectsCount.addAndGet(report.getSuccessObjectsCount());
        failedCheckObjectsCount.addAndGet(report.getFailedCheckObjectsCount());
        notFoundObjectsCount.addAndGet(report.getNotFoundObjectsCount());
        containersCheckedCount.addAndGet(report.getContainersCheckedCount());
        notFoundObjects.addAll(report.getNotFoundObjects());
    }
}
