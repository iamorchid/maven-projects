package will.test.morphia;

//import dev.morphia.annotations.*;
//import org.bson.types.ObjectId;

//@Entity("employees")
//@Indexes(
//        @Index(value = "group_name", fields = {@Field("group"), @Field("name")}, options = @IndexOptions(unique = true))
//)

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

@Entity("employees")
@Indexes(
        @Index(fields = {@Field("group"), @Field("name")}, options = @IndexOptions(unique = true, name = "gorup"))
)
class Employee {
    @Id
    private ObjectId id;

    private String group;

    private String name;

    @Property("wage")
    private Double salary;

    public Employee() {

    }

    public Employee(String group, String name, double salary) {
        this.group = group;
        this.name = name;
        this.salary = salary;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }
}
