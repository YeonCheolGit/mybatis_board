package main.service.member;

import lombok.extern.log4j.Log4j2;
import main.DAO.member.MemberDAO;
import main.DTO.MemberDTO;
import main.security.PrincipalDetails;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Service
@Log4j2
public class MemberServiceImpl implements MemberService, UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    private final MemberDAO memberDAO;

    @Autowired
    public MemberServiceImpl(PasswordEncoder passwordEncoder, MemberDAO memberDAO) {
        this.passwordEncoder = passwordEncoder;
        this.memberDAO = memberDAO;
    }

    @Override
    public MemberDTO securityLogin(String username) {
        return memberDAO.securityLogin(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("======================= loadUserByUsername ======================= ");

        MemberDTO memberDTO = memberDAO.securityLogin(username);
        if (memberDTO != null) {
            return new PrincipalDetails(memberDTO);
        }
        return null;
    }

    @Override
    public List<MemberDTO> allMemberList() {
        return memberDAO.allMemberList();
    }


    /*
     * 회원가입 버튼 클릭 시 동작
     * 중복체크 버튼 누르지 않는 경우 대비
     * 1. 중복 email 체크
     * 2. 중복 nickName 체크
     * if 중복된 email && nickName 경우 X 경우 then 비밀번호 인코딩 후 DB 저장 return true;
     * else return false;
     */
    @Override
    public Boolean registerMember(MemberDTO memberDTO) {
        int resultEmail = memberDAO.duplicatedEmailChk(memberDTO); // 중복 email 체크
        int resultNickName = memberDAO.duplicatedNickNameChk(memberDTO); // 중복 nickName 체크

        if (resultNickName == 0 && resultEmail == 0) { // 중복된 email && nickName X 경우
            String rawPwd = memberDTO.getPassword(); // 사용자가 입력한 raw 비밀번호
            String encodedPwd = passwordEncoder.encode(rawPwd); // raw 비밀번호를 인코딩
            memberDTO.setPassword(encodedPwd);

            memberDAO.registerMember(memberDTO);
            return true;
        } else {
            return false;
        }
    }

    /*
     * 로그인 클릭 시 동작
     * if 이메일 일치 X || 비밀번호 일치 X || 정지 회원 경우 return null;
     * else return MemberDTO 객체;
     */
    @Override
    public Object login(MemberDTO memberDTO, HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();

            MemberDTO dbMember = memberDAO.login(memberDTO);
            boolean pwdMatch = passwordEncoder.matches(memberDTO.getPassword(), dbMember.getPassword()); // 사용자 입력 pwd, 찾아온 DB pwd 비교

            if (dbMember.getUsername() == null || !pwdMatch) { // 일치 X 경우
                session.setAttribute("member", null); // session null
                return "null";
            } if (dbMember.getEnabled() == 0) { // 일치 X 경우
                session.setAttribute("member", null); // session null
                return "1";
            } else { // 일치한 경우
                session.setAttribute("member", dbMember);
                if (dbMember.getRole().equals("ROLE_ADMIN")) { // 일치 && ROLE_ADMIN 경우
                    session.setAttribute("admin_session", dbMember.getRole()); // admin session 등록
                }
                return dbMember;
            }
        } catch (Exception e) {
            log.debug(e);
            return "null";
        }
    }

    @Override
    public int duplicatedNickNameChk(MemberDTO memberDTO) {
        return memberDAO.duplicatedNickNameChk(memberDTO);
    }

    @Override
    public int duplicatedEmailChk(MemberDTO memberDTO) {
        return memberDAO.duplicatedEmailChk(memberDTO);
    }

    @Override
    public int enabledControl(MemberDTO memberDTO) {
         return memberDAO.enabledPause(memberDTO);
    }

    /*
     * 회원정보 받아서 비밀번호 업데이트
     * 1. 사용자 입력 새 pwd 인코딩
     * 2. DB에 새 pwd 저장
     * 3. 로그아웃 - 세션 만료
     * 4. return main 페이지
     */
    @Override
    public Boolean updateMember(MemberDTO memberDTO, HttpServletRequest request) {
        String encodedPwd = passwordEncoder.encode(memberDTO.getPassword()); // 사용자 입력 비밀번호 인코딩
        memberDTO.setPassword(encodedPwd); // 인코딩 된 비밀번호 저장

        try {
            memberDAO.updateMember(memberDTO); // 인코딩 된 비밀번호로 회원정보 업데이트
            HttpSession session = request.getSession();
            session.invalidate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
     * 임시 비밀번호 전송
     */
    @Override
    public void sendEmail(MemberDTO memberDTO, String div) {
        String charSet = "utf-8";
        String hostSMTP = "smtp.gmail.com";
        String hostSMTPid = "yeoncheol.jang@gmail.com";
        String hostSMTPpwd = "duscjf135789**";

        // 보내는 사람 EMail, 제목, 내용
        String fromEmail = "yeoncheol.jang@gmail.com";
        String fromName = "와인검색 사이트";
        String subject = "";
        String msg = "";

        if(div.equals("findPwd")) {
            subject = "와인검색 사이트 임시 비밀번호 안내";
            msg += "<div align='center' style='border:1px solid black; font-family:verdana'>";
            msg += "<h3 style='color: blue;'>";
            msg += memberDTO.getNickName() + "님의 임시 비밀번호 입니다. 비밀번호를 변경하여 사용하세요.</h3>";
            msg += "<p>임시 비밀번호 : ";
            msg += memberDTO.getPassword() + "</p></div>";
        }

        // 받는 사람 E-Mail 주소 (회원가입 시 email로)
        String mail = memberDTO.getUsername();
        try {
            HtmlEmail email = new HtmlEmail();
            email.setDebug(true);
            email.setCharset(charSet);
            email.setSSL(true);
            email.setHostName(hostSMTP);
            email.setSmtpPort(465); //네이버 이용시 587

            email.setAuthentication(hostSMTPid, hostSMTPpwd);
            email.setTLS(true);
            email.addTo(mail, charSet);
            email.setFrom(fromEmail, fromName, charSet);
            email.setSubject(subject);
            email.setHtmlMsg(msg);
            email.send();
        } catch (Exception e) {
            log.debug(e);
        }
    } // sendEmail() 끝

    /*
     * 1. 찾으려는 비밀번호의 이메일, 닉네임 검증
     * 2. 임시 비밀번호 생성
     * 3. sendEmail() <-- raw 비밀번호 전송
     * 4. 인코딩 된 비밀번호 DB 저장
     */
    @Override
    public String findPwd(MemberDTO memberDTO) {
        try {
            // 아이디 && 닉네임 일치 X
            if (memberDAO.duplicatedEmailChk(memberDTO) == 0 && memberDAO.duplicatedNickNameChk(memberDTO) == 0) {
                log.debug("==================== 등록 X 이메일 & 닉네임 =================");
                return "1";
            }
            // 이메일, 아이디 다 있으면
            else {
                // 임시 비밀번호 생성

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.setLength(10);
                for (int i = 0; i < 6; i++) {
                    stringBuilder.append((char) ((Math.random() * 26) + 97)); // ASCII 코드 규칙상 97 = a, 즉 알파벳 26개 랜덤 출력
                    stringBuilder.append((int) (Math.random() * 26)); // 알파벳 뒤 정수 섞기
                }

                // raw 임시 비밀번호 이메일 발송
                memberDTO.setPassword(stringBuilder.toString());
                sendEmail(memberDTO, "findPwd");

                // raw 비밀번호 encode 후 DB 저장
                String encodedPwd = passwordEncoder.encode(stringBuilder);
                memberDTO.setPassword(encodedPwd);
                memberDAO.updateMember(memberDTO);

                return "true";
            }
        } catch (Exception e) {
            log.debug(e);
            return "2";
        }
    } // findPwd() 끝
}
