package com.example.service;

import com.example.common.enums.ResultCodeEnum;
import com.example.entity.Roomplan;
import com.example.exception.CustomException;
import com.example.mapper.CourseMapper;
import com.example.mapper.RoomplanMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教室：编号不能重复；改编号时课程跟着改；还有课排着的教室不能删。
 */
@ExtendWith(MockitoExtension.class)
class RoomplanServiceTest {

    @Mock
    private RoomplanMapper roomplanMapper;
    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private RoomplanService service;

    @Test
    @DisplayName("编号和别的教室重复时不能保存（查重排除自己）")
    void duplicateCodeIsRejected() {
        when(roomplanMapper.selectByCode("7708", 7)).thenReturn(room(2, "7708"));

        CustomException e = assertThrows(CustomException.class, () -> service.updateById(room(7, "7708")));
        assertEquals(ResultCodeEnum.ROOM_CODE_EXIST_ERROR.code, e.getCode());
        verify(roomplanMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("教室改了编号：排在这里的课一起改成新编号；编号没变就不动课程")
    void renamingMovesCourses() {
        when(roomplanMapper.selectById(7)).thenReturn(room(7, "7715"));

        service.updateById(room(7, "7716"));
        verify(courseMapper).renameRoom("7715", "7716");

        Roomplan sameCode = room(7, "7715");
        sameCode.setNum(20);
        service.updateById(sameCode);
        verify(courseMapper, never()).renameRoom("7715", "7715");
    }

    @Test
    @DisplayName("还有没结课的课排在这里的教室不能删；批量删除逐条检查")
    void roomInUseCannotBeDeleted() {
        when(roomplanMapper.selectById(9)).thenReturn(room(9, "7711"));
        when(roomplanMapper.selectById(7)).thenReturn(room(7, "7715"));
        when(courseMapper.countActiveByRoom("7711")).thenReturn(2);
        when(courseMapper.countActiveByRoom("7715")).thenReturn(0);

        CustomException e = assertThrows(CustomException.class, () -> service.deleteBatch(Arrays.asList(7, 9)));
        assertEquals(ResultCodeEnum.ROOM_IN_USE_ERROR.code, e.getCode());
        verify(roomplanMapper, never()).deleteById(9);
        verify(roomplanMapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("不存在的教室删除时不查课程")
    void missingRoomSkipsCourseCheck() {
        service.deleteById(99);
        verify(courseMapper, never()).countActiveByRoom(anyString());
        verify(roomplanMapper).deleteById(99);
    }

    private static Roomplan room(Integer id, String code) {
        Roomplan r = new Roomplan();
        r.setId(id);
        r.setCode(code);
        return r;
    }
}
