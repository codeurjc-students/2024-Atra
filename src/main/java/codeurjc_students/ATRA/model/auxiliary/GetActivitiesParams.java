package codeurjc_students.atra.model.auxiliary;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GetActivitiesParams {
    String from;
    Long id;
    Integer startPage;
    Integer pagesToFetch;
    Integer pageSize;
    String cond;
    String visibility;

}
