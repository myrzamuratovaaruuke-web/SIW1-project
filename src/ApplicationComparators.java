import java.util.Comparator;

public class ApplicationComparators {

    // 1. By company name A-Z
    public static class ByCompanyName implements Comparator<JobApplication> {
        @Override
        public int compare(JobApplication a1, JobApplication a2) {
            return a1.getCompanyName().compareToIgnoreCase(a2.getCompanyName());
        }
    }

    // 2. By position A-Z
    public static class ByPosition implements Comparator<JobApplication> {
        @Override
        public int compare(JobApplication a1, JobApplication a2) {
            return a1.getPosition().compareToIgnoreCase(a2.getPosition());
        }
    }

    // 3. By salary ascending
    public static class BySalaryAscending implements Comparator<JobApplication> {
        @Override
        public int compare(JobApplication a1, JobApplication a2) {
            return Double.compare(a1.getExpectedSalary(), a2.getExpectedSalary());
        }
    }

    // 4. Multi-criteria: status A-Z, then salary descending, then company
    public static class ByStatusThenSalary implements Comparator<JobApplication> {
        @Override
        public int compare(JobApplication a1, JobApplication a2) {
            int result = a1.getStatus().compareToIgnoreCase(a2.getStatus());
            if (result != 0) {
                return result;
            }
            result = Double.compare(a2.getExpectedSalary(), a1.getExpectedSalary());
            if (result != 0) {
                return result;
            }
            return a1.getCompanyName().compareToIgnoreCase(a2.getCompanyName());
        }
    }
}