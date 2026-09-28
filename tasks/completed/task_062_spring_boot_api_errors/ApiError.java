package learning.task062;

import java.util.List;

public record ApiError(String code, List<String> fields) {
}
