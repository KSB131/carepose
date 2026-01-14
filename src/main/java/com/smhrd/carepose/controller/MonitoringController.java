package com.smhrd.carepose.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smhrd.carepose.dto.MonitoringDTO;
import com.smhrd.carepose.entity.PatientEntity;
import com.smhrd.carepose.entity.PositionEntity;
import com.smhrd.carepose.repository.PatientRepository;
import com.smhrd.carepose.repository.PositionRepository;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MonitoringController {

   @Autowired
   private PatientRepository patientRepository;
   
   @Autowired
   private PositionRepository positionRepository;

   @GetMapping("/monitoring")
   public String monitoringPage(Model model, HttpServletRequest request) {
       List<PatientEntity> patientList = patientRepository.findAll();
       model.addAttribute("patients", patientList);

       // 카메라 데이터가 있다면 추가 가능
       model.addAttribute("cameras", null);

       // 사이드바 active 처리용
       model.addAttribute("requestURI", request.getRequestURI());

       return "monitoring";
   }
   
   // -----------------------------
   // ✅ 2시간 타이머 종료 시 DB 시간 갱신용 API
   // -----------------------------
   @PostMapping("/api/updatePositionTime")
   @ResponseBody
   public ResponseEntity<String> updatePositionTime(@RequestParam String bedId) {

       // 침상에 해당하는 PositionEntity 조회
       PositionEntity pos = positionRepository.findByPatientId(bedId);

       if (pos != null) {
           // 현재 시간으로 갱신
           pos.setLastPositionTime(LocalDateTime.now());
           positionRepository.save(pos);
           return ResponseEntity.ok("갱신 완료");
       }

       return ResponseEntity.badRequest().body("해당 침상 없음");
   }

   @GetMapping("/api/monitoringData")
   @ResponseBody
   public ResponseEntity<List<MonitoringDTO>> getMonitoringData() {

       List<MonitoringDTO> result = patientRepository.findAll().stream()
           .map(p -> {
               PositionEntity pos = p.getPosition();
               
               int grade = p.getGrade();  // int 타입
               int totalSeconds;

               switch (grade) {
                   case 1: totalSeconds = 105 * 60; break;  // 1시간45분
                   case 2: totalSeconds = 90 * 60; break;   // 1시간30분
                   case 3: totalSeconds = 80 * 60; break;   // 1시간20분
                   default: totalSeconds = 120 * 60;        // grade 0
               }
               
               return new MonitoringDTO(
                   p.getPatientId(),
                   pos != null ? pos.getLastPosition() : null,
                   pos != null ? pos.getLastPositionTime() : null,
                   grade,
                   totalSeconds
               );
           })
           .toList();

       return ResponseEntity.ok(result);
   }
   
   @GetMapping("/api/latest-image")
   @ResponseBody
   public String getLatestImage(
           @RequestParam String folder,
           @RequestParam String sub,
           @RequestParam String prefix) {

       File dir = new File("C:/carepose-images/images/" + folder + "/" + sub);

       if (!dir.exists() || !dir.isDirectory()) {
           return "";
       }

       Pattern pattern = Pattern.compile(prefix + "(\\d+)\\.jpg");

       return Arrays.stream(dir.listFiles())
               .map(File::getName)
               .map(name -> {
                   Matcher m = pattern.matcher(name);
                   return m.matches()
                           ? new AbstractMap.SimpleEntry<>(name, Integer.parseInt(m.group(1)))
                           : null;
               })
               .filter(Objects::nonNull)
               .max(Comparator.comparingInt(Map.Entry::getValue))
               .map(Map.Entry::getKey)
               .orElse("");
   }
   
}
