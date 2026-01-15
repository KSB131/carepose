package com.smhrd.carepose.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.smhrd.carepose.entity.MemberEntity;
import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.repository.MemberRepository;
import com.smhrd.carepose.repository.PatientRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
   
   
   @Autowired
   MemberRepository memberRepository;
   
   @Autowired
   PatientRepository patientRepository;
   
   @GetMapping("/")
   public String home(){
       return "login";   // login.html 로 이동
   }
   
   @GetMapping("/rooms")
   public String roomSelect(Model model, HttpServletRequest request) {

       // 1. 모든 환자 조회
       List<PatientEntity> patients = patientRepository.findAll();
       model.addAttribute("patients", patients);

       // 2. 동적으로 병실 번호를 추출하여 저장할 Map (층별로 리스트 관리)
       // TreeMap을 사용하면 key(층수)를 기준으로 자동 정렬됩니다.
       Map<Integer, List<Integer>> roomMap = new java.util.TreeMap<>();

       for (PatientEntity p : patients) {
           String pid = p.getPatientId(); // 예: "801A"
           
           if (pid != null && pid.length() >= 3) {
               try {
                   // 앞 3자리 숫자 추출 (예: "801A" -> 801)
                   int roomNum = Integer.parseInt(pid.substring(0, 3));
                   int floor = roomNum / 100; // 층수 추출 (예: 801 / 100 = 8)

                   // 해당 층의 리스트가 없으면 새로 만들고, 병실 번호 추가
                   roomMap.computeIfAbsent(floor, k -> new java.util.ArrayList<>()).add(roomNum);
               } catch (NumberFormatException e) {
                   // 숫자가 아닌 ID 형식은 건너뜁니다.
               }
           }
       }

       // 3. 각 층별 병실 번호 중복 제거 및 정렬
       for (Integer floor : roomMap.keySet()) {
           List<Integer> rooms = roomMap.get(floor);
           // 중복 제거 및 정렬
           List<Integer> sortedRooms = rooms.stream()
                                            .distinct()
                                            .sorted()
                                            .toList();
           roomMap.put(floor, sortedRooms);
       }

       model.addAttribute("roomMap", roomMap);
       model.addAttribute("requestURI", request.getRequestURI());

       return "roomSelect";
   }
   
   

}
