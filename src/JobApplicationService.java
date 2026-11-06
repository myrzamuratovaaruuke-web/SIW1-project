import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JobApplicationService {
    // Block A: List - storage of all applications
    private final List<JobApplication> applicationList = new ArrayList<>();

    // Block B: Map - primary key index, O(1) access by id
    private final Map<Integer, JobApplication> idIndexMap = new HashMap<>();

    // Block B: Map - categorization index, 1:N (one status -> many applications)
    private final Map<String, List<JobApplication>> statusGroupMap = new HashMap<>();

    // N:M: Map - skill -> applications that require it
    private final Map<Skill, Set<JobApplication>> skillIndexMap = new HashMap<>();

    // Block D: Queue - FIFO queue of incoming requests
    private final Queue<JobApplication> processingQueue = new ArrayDeque<>();

    // ---------- CRUD: create ----------

    public void addApplication(JobApplication application) {
        Objects.requireNonNull(application, "Application cannot be null");
        if (idIndexMap.containsKey(application.getId())) {
            throw new IllegalArgumentException(
                    "Application with id " + application.getId() + " already exists");
        }
        applicationList.add(application);
        idIndexMap.put(application.getId(), application);
        statusGroupMap.computeIfAbsent(application.getStatus(), k -> new ArrayList<>()).add(application);
    }

    // ---------- CRUD: read ----------

    public Optional<JobApplication> getById(int id) {
        return Optional.ofNullable(idIndexMap.get(id));
    }

    // ---------- CRUD: update ----------

    public void updateStatus(int id, String newStatus) {
        JobApplication app = requireById(id);
        removeFromStatusGroup(app);
        app.setStatus(newStatus);
        statusGroupMap.computeIfAbsent(newStatus, k -> new ArrayList<>()).add(app);
    }

    public void linkSkill(int id, Skill skill) {
        JobApplication app = requireById(id);
        app.linkSkill(skill);
        skillIndexMap.computeIfAbsent(skill, k -> new HashSet<>()).add(app);
    }

    public void unlinkSkill(int id, Skill skill) {
        JobApplication app = requireById(id);
        app.unlinkSkill(skill);
        removeFromSkillIndex(skill, app);
    }

    // ---------- CRUD: delete (synchronous, from ALL collections) ----------

    public boolean removeApplication(int id) {
        JobApplication app = idIndexMap.remove(id);
        if (app == null) {
            return false;
        }
        applicationList.remove(app);
        processingQueue.remove(app);
        removeFromStatusGroup(app);
        for (Skill skill : app.getRequiredSkills()) {
            removeFromSkillIndex(skill, app);
        }
        return true;
    }

    // ---------- Roles ----------

    // ROLE Applicant: sends a request to the queue
    public void submitApplication(int id) {
        JobApplication app = requireById(id);
        if (processingQueue.contains(app)) {
            throw new IllegalStateException("Application " + id + " is already in the queue");
        }
        updateStatus(id, "Applied");
        processingQueue.offer(app);
    }

    // ROLE HR: takes the next request with poll() (FIFO)
    public Optional<JobApplication> processNextRequest() {
        JobApplication app = processingQueue.poll();
        if (app == null) {
            return Optional.empty();          // queue is empty
        }
        updateStatus(app.getId(), "Processed");
        return Optional.of(app);
    }

    public int getQueueSize() {
        return processingQueue.size();
    }

    // ---------- Search and filter ----------

    public List<JobApplication> getApplicationsByStatus(String status) {
        return new ArrayList<>(statusGroupMap.getOrDefault(status, Collections.emptyList()));
    }

    public List<JobApplication> getApplicationsBySkill(Skill skill) {
        return new ArrayList<>(skillIndexMap.getOrDefault(skill, Collections.emptySet()));
    }

    public List<JobApplication> filter(Predicate<JobApplication> criteria) {
        return applicationList.stream()
                .filter(criteria)
                .collect(Collectors.toList());
    }

    // ---------- Analytics ----------

    // Read-only view: outside code cannot change the internal list
    public List<JobApplication> getAll() {
        return Collections.unmodifiableList(applicationList);
    }

    // Sorted COPY: the internal list keeps its order
    public List<JobApplication> getSorted(Comparator<JobApplication> comparator) {
        List<JobApplication> copy = new ArrayList<>(applicationList);
        copy.sort(comparator);
        return copy;
    }

    public Map<String, Integer> countByStatus() {
        Map<String, Integer> result = new TreeMap<>();
        for (Map.Entry<String, List<JobApplication>> entry : statusGroupMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    public double averageExpectedSalary() {
        if (applicationList.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (JobApplication app : applicationList) {
            sum += app.getExpectedSalary();
        }
        return sum / applicationList.size();
    }

    public Optional<JobApplication> findHighestSalary() {
        return applicationList.stream()
                .max(Comparator.comparingDouble(JobApplication::getExpectedSalary));
    }

    public void showAllApplications() {
        System.out.println("=== All Job Applications ===");
        for (JobApplication app : applicationList) {
            app.displayInfo();
            System.out.println();
        }
    }

    public void printSummary() {
        System.out.println("=== Summary ===");
        System.out.println("Total applications: " + applicationList.size());
        System.out.println("By status: " + countByStatus());
        System.out.println("Distinct skills: " + skillIndexMap.keySet());
        System.out.printf("Average expected salary: %.2f%n", averageExpectedSalary());
        findHighestSalary().ifPresent(a -> System.out.println("Highest salary: " + a));
        System.out.println("Requests waiting in queue: " + processingQueue.size());
    }

    // ---------- Private helpers: keep indexes in sync ----------

    private JobApplication requireById(int id) {
        return getById(id).orElseThrow(
                () -> new NoSuchElementException("Application with id " + id + " not found"));
    }

    private void removeFromStatusGroup(JobApplication app) {
        List<JobApplication> group = statusGroupMap.get(app.getStatus());
        if (group != null) {
            group.remove(app);
            if (group.isEmpty()) {
                statusGroupMap.remove(app.getStatus());
            }
        }
    }

    private void removeFromSkillIndex(Skill skill, JobApplication app) {
        Set<JobApplication> apps = skillIndexMap.get(skill);
        if (apps != null) {
            apps.remove(app);
            if (apps.isEmpty()) {
                skillIndexMap.remove(skill);
            }
        }
    }
}