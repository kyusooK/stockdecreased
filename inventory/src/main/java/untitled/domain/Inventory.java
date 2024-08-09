package untitled.domain;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import lombok.Data;
import untitled.InventoryApplication;

@Entity
@Table(name = "Inventory_table")
@Data
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer stock;

    private String productName;

    public static InventoryRepository repository() {
        InventoryRepository inventoryRepository = InventoryApplication.applicationContext.getBean(
            InventoryRepository.class
        );
        return inventoryRepository;
    }

    public static void decreaseStock(OrderPlaced orderPlaced) {
        repository()
            .findById(orderPlaced.getId())
            .ifPresent(inventory -> {
                inventory.setStock(inventory.getStock() - 1);
                repository().save(inventory);
                StockDecreased stockDecreased = new StockDecreased(inventory);
                stockDecreased.publishAfterCommit();
            });
    }
}
