package untitled.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.Data;

@Data
public class OrderCommand {

    private Long id;
    private String productId;
    private String userId;
    private String productName;
}
