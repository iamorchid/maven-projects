package will.samples;

import lombok.AllArgsConstructor;
import lombok.Getter;
import will.test.message.IBizMessage;

@AllArgsConstructor
@Getter
public class NumberMessage implements IBizMessage {
    private int number;
}
