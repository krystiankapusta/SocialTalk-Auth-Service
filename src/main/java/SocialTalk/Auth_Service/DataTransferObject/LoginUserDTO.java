package SocialTalk.Auth_Service.DataTransferObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserDTO {
    private String email;
    private String password;

}
