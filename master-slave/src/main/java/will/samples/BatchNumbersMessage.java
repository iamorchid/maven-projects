package will.samples;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.message.IBizMessage;

import java.util.List;

@AllArgsConstructor
@Getter
public class BatchNumbersMessage implements IBizMessage {
    private List<Integer> numbers;
}
