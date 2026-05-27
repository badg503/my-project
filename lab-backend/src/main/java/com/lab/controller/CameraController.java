package com.lab.controller;

import com.lab.entity.Camera;
import com.lab.service.CameraService;
import com.lab.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 摄像头管理控制器
 */
@RestController
@RequestMapping("/api/camera")
@Slf4j
public class CameraController {
    
    @Autowired
    private CameraService cameraService;
    
    // ==================== 摄像头管理 ====================
    
    @GetMapping("/list")
    public Result list(
            @RequestParam(required = false) Integer enabled) {
        List<Camera> cameras = cameraService.listCameras(enabled);
        return Result.ok(cameras);
    }
    
    @GetMapping("/listByLab")
    public Result listByLab(
            @RequestParam Long labId,
            @RequestParam(required = false) Integer enabled) {
        List<Camera> cameras = cameraService.listByLabId(labId, enabled);
        return Result.ok(cameras);
    }
    
    @GetMapping("/get/{code}")
    public Result getByCode(
            @PathVariable("code") String cameraCode) {
        Camera camera = cameraService.getByCode(cameraCode);
        return Result.ok(camera);
    }
    
    @PostMapping("/add")
    public Result add(
            @RequestBody Camera camera) {
        cameraService.addCamera(camera);
        return Result.ok("添加成功");
    }
    
    @PutMapping("/update/{code}")
    public Result update(
            @PathVariable("code") String cameraCode,
            @RequestBody Camera camera) {
        cameraService.updateCamera(cameraCode, camera);
        return Result.ok("更新成功");
    }
    
    @DeleteMapping("/delete/{code}")
    public Result delete(
            @PathVariable("code") String cameraCode) {
        cameraService.deleteCamera(cameraCode);
        return Result.ok("删除成功");
    }
    
    @PutMapping("/updateStatus/{code}")
    public Result updateStatus(
            @PathVariable("code") String cameraCode,
            @RequestParam String status) {
        cameraService.updateStatus(cameraCode, status);
        return Result.ok("状态更新成功");
    }
    
    // ==================== 检测操作 ====================
    
    @PostMapping("/detect/{code}")
    public Result detect(
            @PathVariable("code") String cameraCode) {
        String result = cameraService.detectCamera(cameraCode);
        return Result.ok(result);
    }
    
    @PostMapping("/detectAll")
    public Result detectAll() {
        Map<String, Object> result = cameraService.detectAllCameras();
        return Result.ok(result);
    }
    
    // ==================== 检测记录 ====================
    
    @GetMapping("/logs")
    public Result logs(
            @RequestParam(required = false) String cameraCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer hasAlert) {
        
        // 处理日期格式
        String startTime = null;
        String endTime = null;
        
        if (startDate != null) {
            startTime = startDate + " 00:00:00";
        }
        if (endDate != null) {
            endTime = endDate + " 23:59:59";
        }
        
        List<Map<String, Object>> logs = cameraService.getLogs(cameraCode, startTime, endTime, hasAlert);
        return Result.ok(logs);
    }
    
    @PutMapping("/processAlert/{id}")
    public Result processAlert(
            @PathVariable("id") Long logId,
            @RequestParam Long userId,
            @RequestParam String remark) {
        cameraService.processAlert(logId, userId, remark);
        return Result.ok("处理成功");
    }
}
