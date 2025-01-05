package SocialTalk.Auth_Service.DataTransferObject;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyUserDTO {
    private String email;
    private String verificationCode;
}

