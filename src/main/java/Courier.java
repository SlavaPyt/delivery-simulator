public class Courier {
    private String name;
    private String phone;
    private CourierStatus status;
    public enum CourierStatus{
        BUSY,
        ON_BREAK,
        OFFLINE,
        AVAILABLE
    }
    public String getName(){
        return name;
    }
    public String getPhone(){
        return phone;
    }
    public void setPhone(String phone){
        this.phone = phone;
    }
    public CourierStatus getStatus(){
        return status;
    }

    public void setName(String name){
        this.name = name;
    }

    public Courier (String name, String phone){
        this.name = name;
        this.phone = phone;
        this.status = CourierStatus.AVAILABLE;

    }

}
