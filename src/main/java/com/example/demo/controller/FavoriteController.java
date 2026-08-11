package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.annotation.RequiresRoles;
import com.example.demo.common.Result;
import com.example.demo.entity.Favorite;
import com.example.demo.entity.Room;
import com.example.demo.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 添加收藏（仅租客可调用）
     */
    @PostMapping("/add")
    @RequiresRoles({1, 2})
    public Result<String> addFavorite(HttpServletRequest request, @RequestBody Map<String, Long> params) {
        Long userId = (Long) request.getAttribute("realUserId");
        Long roomId = params.get("roomId");
        if (roomId == null) {
            return Result.fail("房源ID不能为空");
        }
        try {
            boolean success = favoriteService.addFavorite(userId, roomId);
            return success ? Result.success("收藏成功") : Result.fail("收藏失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 取消收藏（仅租客可调用）
     */
    @PostMapping("/cancel")
    @RequiresRoles({1, 2})
    public Result<String> cancelFavorite(HttpServletRequest request, @RequestBody Map<String, Long> params) {
        Long userId = (Long) request.getAttribute("realUserId");
        Long roomId = params.get("roomId");
        if (roomId == null) {
            return Result.fail("房源ID不能为空");
        }
        try {
            boolean success = favoriteService.cancelFavorite(userId, roomId);
            return success ? Result.success("取消收藏成功") : Result.fail("取消收藏失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 分页查询我的收藏列表（仅租客可调用）
     */
    @GetMapping("/my")
    @RequiresRoles({1, 2})
    public Result<IPage<Favorite>> getMyFavorites(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<Favorite> page = favoriteService.getMyFavorites(userId, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 查询当前用户是否已收藏该房源（用于前端显示按钮状态）
     */
    @GetMapping("/check")
    @RequiresRoles({1, 2})
    public Result<Boolean> checkFavorited(HttpServletRequest request, @RequestParam Long roomId) {
        Long userId = (Long) request.getAttribute("realUserId");
        boolean favorited = favoriteService.isFavorited(userId, roomId);
        return Result.success(favorited);
    }

    /**
     * 分页查询我的收藏房源列表（对齐前端 my/Favorites.vue）
     */
    @GetMapping("/list")
    @RequiresRoles({1, 2})
    public Result<IPage<Room>> getFavoriteRooms(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "8") Integer pageSize,
            @RequestParam(defaultValue = "newest") String sortBy) {
        Long userId = (Long) request.getAttribute("realUserId");
        IPage<Room> page = favoriteService.getFavoriteRoomPage(userId, pageNum, pageSize, sortBy);
        return Result.success(page);
    }
}