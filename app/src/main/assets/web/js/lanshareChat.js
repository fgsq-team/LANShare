// ============================================================
// 全局变量
// ============================================================
var connected = false;
var onLineDeviceList = [];
var selectedDevice = '';
var selectedDeviceName = '';
var uploadData = null;
var chatMessages = [];
var chatMultiSelectMode = false;
var chatSelectedCount = 0;
var chatMsgIdCounter = 0;
var chatNoSave = false;

// ============================================================
// 聊天文件上传
// ============================================================

function uploadChatFile(formData, address, onSuccess, onError) {
    var ajax = new XMLHttpRequest();
    ajax.open('POST', '/chatUploadFile?address=' + address, true);
    ajax.setRequestHeader('token', localStorage.getItem('token'));
    ajax.upload.addEventListener('progress', function () {}, false);
    ajax.send(formData);
    ajax.onreadystatechange = function () {
        if (ajax.readyState === 4) {
            if (ajax.status >= 200 && ajax.status < 300 || ajax.status === 304) {
                onSuccess(ajax.responseText);
            } else if (ajax.status >= 302) {
                window.location.href = ajax.getResponseHeader('Location');
            } else if (ajax.status === 500) {
                onError(ajax.responseText);
            } else {
                onError('上传文件失败');
            }
        }
    };
}

function startSendFile(deviceAddress) {
    $('#devSelectDialog').modal('hide');

    function onSuccess(result) {
        lightyear.notify(result, 'success', 1000);
        uploadData = null;
    }

    function onError(error) {
        lightyear.notify(error, 'danger', 1000);
        uploadData = null;
    }

    // 本机设备（address 为空）走本地上传，远程设备走聊天上传
    if (deviceAddress == null || deviceAddress.length <= 0) {
        uploadFile(uploadData, onSuccess, onError);
    } else {
        uploadChatFile(uploadData, deviceAddress, onSuccess, onError);
    }
}

// ============================================================
// DOM Ready - 聊天事件绑定
// ============================================================

$(function () {
    var chatContent = document.querySelector('.chat_content');
    var chatUploadModal = document.querySelector('#chatUploadModal');

    // 拖拽文件到聊天区域
    chatContent.addEventListener('dragenter', function () {
        $('#chatUploadModal').modal('show');
        uploadData = null;
    }, false);

    chatUploadModal.addEventListener('dragover', function (e) {
        e.preventDefault();
    }, false);

    chatUploadModal.addEventListener('dragenter', function () {
        $('#chatUploadModal').modal('show');
        uploadData = null;
    }, false);

    chatUploadModal.addEventListener('dragleave', function () {
        $('#chatUploadModal').modal('hide');
    }, false);

    chatUploadModal.addEventListener('drop', function (e) {
        e.preventDefault();
        $('#chatUploadModal').modal('hide');
        uploadData = new FormData();
        Array.from(e.dataTransfer.files).forEach(function (file) {
            uploadData.append('file', file);
        });
        populateDeviceSelect();
        $('#devSelectDialog').modal('show');
    }, false);

    // 输入框键盘事件
    $('.chat_context_edit').keydown(function (e) {
        if (e.keyCode === 13 && e.shiftKey) {
            // Shift+Enter：插入换行
            e.preventDefault();
            var cursorPos = this.selectionStart;
            var text = $(this).val();
            $(this).val(text.substring(0, cursorPos) + '\r\n' + text.substring(cursorPos));
            this.setSelectionRange(cursorPos + 1, cursorPos + 1);
            $(this).scrollTop($(this)[0].scrollHeight);
        } else if (e.keyCode === 13 && e.ctrlKey) {
            // Ctrl+Enter：发送并保留换行
            sendMessage(true);
        } else if (e.keyCode === 13) {
            // Enter：发送
            e.preventDefault();
            sendMessage(false);
        }
    });

    // 不保存消息设置
    var savedNoSave = localStorage.getItem('chatNoSave') === '1';
    $('#chatNoSaveCheckbox').prop('checked', savedNoSave);
    if (savedNoSave) {
        document.body.classList.add('chat-body-no-save');
    }
    $('#chatNoSaveCheckbox').on('change', function () {
        applyChatNoSave();
    });

    // 多选模式下点击消息条目切换选中
    $(document).on('click', '.chat-item-wrapper', function () {
        if (!chatMultiSelectMode) return;
        var $wrapper = $(this);
        var msgId = $wrapper.data('msg-id');
        if ($wrapper.hasClass('chat-msg-selected')) {
            chatSelectedCount--;
            $wrapper.removeClass('chat-msg-selected');
        } else {
            chatSelectedCount++;
            $wrapper.addClass('chat-msg-selected');
        }
        updateChatSelectAllState();
    });
});

// ============================================================
// 消息渲染
// ============================================================

/** 根据消息类型构建内容 DOM */
function buildMessageContent(msg, side) {
    var $content;
    var token = localStorage.getItem('token');

    if (msg.messageType === 0) {
        // 文本消息
        var html = msg.message.replace(/\n/g, '<br>');
        $content = $('<div class="chat_' + side + '_content"></div>').html(html);
    } else if (msg.messageType === 1) {
        // 图片消息
        $content = $('<img src="/file/' + msg.message + '?path=' + msg.filePath + '&token=' + token +
            '" class="chat_' + side + '_content_img" onclick="openFile(\'' + msg.message + '\',\'' + msg.filePath + '\')">');
    } else if (msg.messageType === 2) {
        // 文件消息
        var iconClass = msg.isFile ? 'file_icon' : 'dir_file_icon';
        $content = $('<div class="chat_' + side + '_file_content" onclick="openFile(\'' + msg.message + '\',\'' + msg.filePath + '\')">' +
            '<div class="' + iconClass + '"></div>' +
            '<div class="file_info_content">' +
            '<div class="file_info_name">' + msg.message + '</div>' +
            '<div class="file_info_size">' + msg.fileSize + '</div>' +
            '</div></div>');
    }
    return $content;
}

function renderMessageBubble(msg) {
    var side = msg.isLeft ? 'left' : 'right';

    // 设备图标
    var $avatar;
    if (msg.devType === 0) {
        $avatar = $('<div class="chat_profile_photo_' + side + ' ic_phone"></div>');
    } else {
        $avatar = $('<div class="chat_profile_photo_' + side + ' ic_win"></div>');
    }

    // 消息内容区
    var $content = $('<div class="chat_item_content_' + side + '"/>');
    $content.append($('<div class="chat_' + side + '_nick clearfix">' + msg.devName + '</div>'));
    $content.append(buildMessageContent(msg, side));

    // 消息气泡容器
    var $bubble = $('<div class="chat_' + side + '"></div>');
    $bubble.append($avatar);
    $bubble.append($content);
    return $bubble;
}

function addMessage(msg) {
    var $bubble = renderMessageBubble(msg);

    // 包装容器（用于多选定位）
    var msgId = 'chat-msg-' + (++chatMsgIdCounter);
    var wrapperClass = 'chat-item-wrapper' + (msg.isLeft ? '' : ' chat-right-wrapper');
    var $wrapper = $('<div class="' + wrapperClass + '" data-msg-id="' + msgId + '"></div>');
    $wrapper.append($bubble);

    // 追加到聊天区域并滚动到底部
    var $chatContent = $('.chat_content');
    $chatContent.append($wrapper);
    var chatEl = document.getElementById('chat_content');
    chatEl.scroll({top: chatEl.scrollHeight, behavior: 'smooth'});

    // 缓存消息（不保存消息模式下不缓存）
    if (!chatNoSave) {
        msg._id = msgId;
        chatMessages.push(msg);
        saveChatCache();
    }
}

// ============================================================
// WebSocket 连接管理
// ============================================================

var ws = null;
var heartbeatTimer = null;
var reconnectTimer = null;
var isReconnecting = false;

function startHeartbeat() {
    stopHeartbeat();
    heartbeatTimer = setInterval(function () {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({cmd: PING}));
        }
    }, 30000);
}

function stopHeartbeat() {
    if (heartbeatTimer) {
        clearInterval(heartbeatTimer);
        heartbeatTimer = null;
    }
}

function scheduleReconnect() {
    if (reconnectTimer) return;
    reconnectTimer = setTimeout(function () {
        reconnectTimer = null;
        initWS();
    }, 5000);
}

function initWS() {
    stopHeartbeat();
    ws = new WebSocket('ws://' + window.location.host + '/wss?token=' + localStorage.getItem('token'));

    ws.onopen = function () {
        connected = true;
        startHeartbeat();
        if (isReconnecting) {
            isReconnecting = false;
            if (reconnectTimer) {
                clearTimeout(reconnectTimer);
                reconnectTimer = null;
            }
            lightyear.notify('已重新连接成功', 'success', 1500);
        }
        $('#notConnectedModal').modal('hide');
    };

    ws.onmessage = function (event) {
        var data = JSON.parse(event.data);

        if (data.cmd === SEND_MSSAGE) {
            addMessage({
                isLeft: data.isLeft,
                devName: data.devName,
                devType: data.devType,
                message: data.message,
                messageType: data.messageType,
                filePath: data.filePath,
                fileSize: data.fileSize
            });
            if (chatNoSave) {
                clearChatCache();
            }
            return;
        }

        if (data.cmd === SYNC_DEVICE_LIST) {
            onLineDeviceList = data.data;
            var $selector = $('.device_selector');
            $selector.empty();
            $selector.append('<option value="">所有设备</option>');
            for (var i = 0; i < onLineDeviceList.length; i++) {
                var device = onLineDeviceList[i];
                var $option = $('<option value="' + device.address + '">' + device.devName + '</option>');
                if (device.address === selectedDevice) {
                    $option.attr('selected', 'selected');
                }
                $selector.append($option);
            }
            return;
        }

        if (data.cmd === CHANGE_THEME) {
            applyTheme(data.theme);
            lightyear.notify('主题已切换为：' + getThemeName(data.theme), 'success', 1500);
            return;
        }

        if (data.cmd === DRAW_EVENT) {
            handleDrawEvent(data);
        }
    };

    ws.onclose = function () {
        connected = false;
        stopHeartbeat();
        if (!isReconnecting) {
            isReconnecting = true;
            lightyear.notify('与客户端连接断开，正在尝试重连...', 'danger', 2000);
            $('#notConnectedModal').modal({backdrop: 'static', show: true});
        }
        scheduleReconnect();
    };

    ws.onerror = function () {
        connected = false;
        stopHeartbeat();
    };
}

// ============================================================
// 发送消息
// ============================================================

function sendMessage(isClip) {
    if (!connected) {
        lightyear.notify('未连接客户端，请启动LANShare后刷新页面！', 'danger', 1000);
        return;
    }

    var $input = $('.chat_context_edit');
    var text = $input.val();
    if (text == null || text.length === 0) {
        lightyear.notify('输入不能为空', 'danger', 1000);
        return;
    }

    var displayName = (selectedDeviceName != null && selectedDeviceName.length > 0)
        ? selectedDeviceName + ' ← 我'
        : '所有设备 ← 我';

    var message = {
        cmd: SEND_MSSAGE,
        isLeft: false,
        devName: displayName,
        devType: 0,
        message: text,
        messageType: 0,
        selectedDevice: selectedDevice,
        isClip: isClip
    };

    addMessage(message);
    if (chatNoSave) {
        clearChatCache();
    }
    ws.send(JSON.stringify(message));
    $input.val('');
    $input[0].style.height = '40px';
}

// ============================================================
// 发送按钮事件（长按/短按）
// ============================================================

var pressTimer;

$('.send_massage').on('mousedown', function (e) {
    e.preventDefault();
    pressTimer = setTimeout(function () {
        sendMessage(true);
        clearTimeout(pressTimer);
        pressTimer = null;
    }, 500);
}).on('mouseup', function () {
    if (pressTimer != null) {
        clearTimeout(pressTimer);
        sendMessage(false);
    }
});

$('.device_selector').on('change', function () {
    selectedDevice = $('.device_selector').val();
    selectedDeviceName = $('.device_selector option:selected').text();
});

var sendBtn = document.getElementById('send_massage');
sendBtn.onmousedown = function (e) {
    e.preventDefault();
};

// ============================================================
// 消息缓存管理
// ============================================================

function saveChatCache() {
    if (chatNoSave) return;
    try {
        localStorage.setItem('chatMessages', JSON.stringify(chatMessages));
    } catch (e) {}
}

function loadChatFromCache() {
    if (chatNoSave) return;
    try {
        var cached = localStorage.getItem('chatMessages');
        if (cached) {
            chatMessages = JSON.parse(cached);
            chatMessages.forEach(function (msg) {
                if (!msg._id) {
                    msg._id = 'chat-msg-' + (++chatMsgIdCounter);
                }
                var $bubble = renderMessageBubble(msg);
                var wrapperClass = 'chat-item-wrapper' + (msg.isLeft ? '' : ' chat-right-wrapper');
                var $wrapper = $('<div class="' + wrapperClass + '" data-msg-id="' + msg._id + '"></div>');
                $wrapper.append($bubble);
                $('.chat_content').append($wrapper);
            });
            if (chatMultiSelectMode) {
                $('.chat_content').addClass('chat-multi-select-mode');
            }
        }
    } catch (e) {
        chatMessages = [];
    }
}

function clearChatCache() {
    chatMessages = [];
    localStorage.removeItem('chatMessages');
}

// ============================================================
// 消息删除
// ============================================================

function deleteChatMessage(msgId) {
    $('[data-msg-id="' + msgId + '"]').remove();
    chatMessages = chatMessages.filter(function (m) { return m._id !== msgId; });
    saveChatCache();
}

function deleteSelectedMessages() {
    var ids = [];
    $('.chat-item-wrapper.chat-msg-selected').each(function () {
        ids.push($(this).data('msg-id'));
    });
    if (ids.length === 0) {
        lightyear.notify('请先选择要删除的消息', 'info', 1000);
        return;
    }
    ids.forEach(function (id) {
        $('[data-msg-id="' + id + '"]').remove();
    });
    chatMessages = chatMessages.filter(function (m) { return ids.indexOf(m._id) === -1; });
    saveChatCache();
    chatSelectedCount = 0;
    updateChatSelectAllState();
    lightyear.notify('已删除 ' + ids.length + ' 条消息', 'success', 1000);
}

function clearAllChatMessages() {
    $('.chat_content').empty();
    chatMessages = [];
    clearChatCache();
    chatSelectedCount = 0;
    lightyear.notify('已清空所有消息', 'success', 1000);
}

// ============================================================
// 多选模式
// ============================================================

function toggleChatMultiSelect(enable) {
    chatMultiSelectMode = enable;
    chatSelectedCount = 0;
    if (enable) {
        $('.chat_content').addClass('chat-multi-select-mode');
        $('#chatToolbar').show();
    } else {
        $('.chat_content').removeClass('chat-multi-select-mode');
        $('.chat-item-wrapper').removeClass('chat-msg-selected');
        $('#chatToolbar').hide();
    }
    updateChatSelectAllState();
}

function toggleChatSelectAll(checkbox) {
    var isChecked = $(checkbox).is(':checked');
    if (isChecked) {
        chatSelectedCount = $('.chat-item-wrapper').length;
        $('.chat-item-wrapper').addClass('chat-msg-selected');
    } else {
        chatSelectedCount = 0;
        $('.chat-item-wrapper').removeClass('chat-msg-selected');
    }
}

function updateChatSelectAllState() {
    var total = $('.chat-item-wrapper').length;
    var $selectAll = $('#chatSelectAllCheckbox');
    if (total === 0) {
        $selectAll.prop('checked', false);
        return;
    }
    var allChecked = chatSelectedCount > 0 && chatSelectedCount === total;
    $selectAll.prop('checked', allChecked);
}

// ============================================================
// 不保存消息设置
// ============================================================

function applyChatNoSave() {
    chatNoSave = $('#chatNoSaveCheckbox').is(':checked');
    localStorage.setItem('chatNoSave', chatNoSave ? '1' : '0');
    if (chatNoSave) {
        // 只阻止新消息缓存，不清空已有消息
        document.body.classList.add('chat-body-no-save');
    } else {
        document.body.classList.remove('chat-body-no-save');
    }
}

// ============================================================
// 远程绘图 Canvas 逻辑（双向）
// ============================================================

var drawCanvas = null;
var drawCtx = null;
var remoteCanvas = null;
var remoteCtx = null;
var drawInitialized = false;
var webDrawColor = '#ff0000';
var webDrawStrokeWidth = 3;
var webIsDrawing = false;

function initDrawCanvas() {
    if (drawInitialized) return;
    drawCanvas = document.getElementById('drawCanvas');
    remoteCanvas = document.getElementById('drawRemoteCanvas');
    if (!drawCanvas || !remoteCanvas) return;
    drawCtx = drawCanvas.getContext('2d');
    remoteCtx = remoteCanvas.getContext('2d');
    resizeDrawCanvas();
    bindDrawEvents();
    drawInitialized = true;
}

function resizeDrawCanvas() {
    if (!drawCanvas || !remoteCanvas) return;
    var container = drawCanvas.parentElement;
    var width = container.clientWidth;
    var height = container.clientHeight;
    drawCanvas.width = width;
    drawCanvas.height = height;
    remoteCanvas.width = width;
    remoteCanvas.height = height;
}

function getDrawPos(event) {
    var rect = drawCanvas.getBoundingClientRect();
    var x = event.clientX - rect.left;
    var y = event.clientY - rect.top;
    return {x: x, y: y, nx: x / drawCanvas.width, ny: y / drawCanvas.height};
}

function getDrawPosTouch(event) {
    var rect = drawCanvas.getBoundingClientRect();
    var touch = event.touches[0];
    var x = touch.clientX - rect.left;
    var y = touch.clientY - rect.top;
    return {x: x, y: y, nx: x / drawCanvas.width, ny: y / drawCanvas.height};
}

function bindDrawEvents() {
    if (!drawCanvas) return;

    // 鼠标事件
    drawCanvas.addEventListener('mousedown', function (e) {
        e.preventDefault();
        webIsDrawing = true;
        var pos = getDrawPos(e);
        sendWebDraw('start', pos.nx, pos.ny);
        drawLocal('start', pos.x, pos.y, webDrawColor, webDrawStrokeWidth);
    });

    drawCanvas.addEventListener('mousemove', function (e) {
        if (!webIsDrawing) return;
        e.preventDefault();
        var pos = getDrawPos(e);
        sendWebDraw('move', pos.nx, pos.ny);
        drawLocal('move', pos.x, pos.y, webDrawColor, webDrawStrokeWidth);
    });

    drawCanvas.addEventListener('mouseup', function (e) {
        if (!webIsDrawing) return;
        e.preventDefault();
        webIsDrawing = false;
        var pos = getDrawPos(e);
        sendWebDraw('end', pos.nx, pos.ny);
        drawLocal('end', pos.x, pos.y, webDrawColor, webDrawStrokeWidth);
    });

    drawCanvas.addEventListener('mouseleave', function (e) {
        if (!webIsDrawing) return;
        webIsDrawing = false;
        var pos = getDrawPos(e);
        sendWebDraw('end', pos.nx, pos.ny);
        drawLocal('end', pos.x, pos.y, webDrawColor, webDrawStrokeWidth);
    });

    // 触摸事件
    drawCanvas.addEventListener('touchstart', function (e) {
        e.preventDefault();
        webIsDrawing = true;
        var pos = getDrawPosTouch(e);
        sendWebDraw('start', pos.nx, pos.ny);
        drawLocal('start', pos.x, pos.y, webDrawColor, webDrawStrokeWidth);
    });

    drawCanvas.addEventListener('touchmove', function (e) {
        if (!webIsDrawing) return;
        e.preventDefault();
        var pos = getDrawPosTouch(e);
        sendWebDraw('move', pos.nx, pos.ny);
        drawLocal('move', pos.x, pos.y, webDrawColor, webDrawStrokeWidth);
    });

    drawCanvas.addEventListener('touchend', function (e) {
        if (!webIsDrawing) return;
        webIsDrawing = false;
        var rect = drawCanvas.getBoundingClientRect();
        var touch = e.changedTouches[0];
        var x = touch.clientX - rect.left;
        var y = touch.clientY - rect.top;
        sendWebDraw('end', x / drawCanvas.width, y / drawCanvas.height);
        drawLocal('end', x, y, webDrawColor, webDrawStrokeWidth);
    });

    // 工具栏事件
    var colorPicker = document.getElementById('drawColorPicker');
    if (colorPicker) {
        colorPicker.addEventListener('input', function () {
            webDrawColor = this.value;
        });
    }

    var strokeRange = document.getElementById('drawStrokeRange');
    if (strokeRange) {
        strokeRange.addEventListener('input', function () {
            webDrawStrokeWidth = parseInt(this.value);
        });
    }
}

// ============================================================
// 绘图 - 本地绘制与远程同步
// ============================================================

function drawLocal(action, x, y, color, strokeWidth) {
    if (!drawCtx) return;
    drawCtx.lineCap = 'round';
    drawCtx.lineJoin = 'round';

    if (action === 'start') {
        drawCtx.beginPath();
        drawCtx.moveTo(x, y);
        drawCtx.strokeStyle = color;
        drawCtx.lineWidth = strokeWidth;
    } else if (action === 'move') {
        drawCtx.lineTo(x, y);
        drawCtx.stroke();
    } else if (action === 'end') {
        drawCtx.lineTo(x, y);
        drawCtx.stroke();
        drawCtx.closePath();
    }
}

function sendWebDraw(action, normalizedX, normalizedY) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    var colorInt = hexToColorInt(webDrawColor);
    var msg = {
        cmd: DRAW_EVENT,
        action: action,
        x: normalizedX,
        y: normalizedY,
        color: colorInt,
        strokeWidth: webDrawStrokeWidth,
        from: 'web'
    };
    ws.send(JSON.stringify(msg));
}

function hexToColorInt(hex) {
    hex = hex.replace('#', '');
    var r = parseInt(hex.substring(0, 2), 16);
    var g = parseInt(hex.substring(2, 4), 16);
    var b = parseInt(hex.substring(4, 6), 16);
    // 转为 Android signed int (AARRGGBB)
    var value = (255 << 24) | (r << 16) | (g << 8) | b;
    return value | 0; // 强制转 signed
}

function handleDrawEvent(data) {
    // 防止回显：忽略来自其他网页客户端的事件
    if (data.from === 'web') return;

    if (!drawInitialized) {
        initDrawCanvas();
    }
    if (!remoteCtx || !remoteCanvas) return;

    var action = data.action;
    var x = data.x * remoteCanvas.width;
    var y = data.y * remoteCanvas.height;

    var statusEl = document.getElementById('draw_status');
    if (statusEl && action === 'start') {
        statusEl.textContent = 'APP正在绘图...';
        statusEl.style.color = '#4CAF50';
    }

    // Android int 转 hex 颜色
    var hex = (data.color >>> 0).toString(16);
    hex = hex.length > 6 ? hex.slice(-6) : hex.padStart(6, '0');
    var cssColor = '#' + hex;

    remoteCtx.lineCap = 'round';
    remoteCtx.lineJoin = 'round';

    if (action === 'start') {
        remoteCtx.beginPath();
        remoteCtx.moveTo(x, y);
        remoteCtx.strokeStyle = cssColor;
        remoteCtx.lineWidth = data.strokeWidth;
    } else if (action === 'move') {
        remoteCtx.lineTo(x, y);
        remoteCtx.stroke();
    } else if (action === 'end') {
        remoteCtx.lineTo(x, y);
        remoteCtx.stroke();
        remoteCtx.closePath();
    } else if (action === 'clear') {
        remoteCtx.clearRect(0, 0, remoteCanvas.width, remoteCanvas.height);
        if (drawCtx && drawCanvas) {
            drawCtx.clearRect(0, 0, drawCanvas.width, drawCanvas.height);
        }
        if (statusEl) {
            statusEl.textContent = '画布已清空';
            statusEl.style.color = '#999';
        }
    }
}

function clearDrawCanvas() {
    if (drawCtx && drawCanvas) {
        drawCtx.clearRect(0, 0, drawCanvas.width, drawCanvas.height);
    }
    if (remoteCtx && remoteCanvas) {
        remoteCtx.clearRect(0, 0, remoteCanvas.width, remoteCanvas.height);
    }
    var statusEl = document.getElementById('draw_status');
    if (statusEl) {
        statusEl.textContent = '画布已清空';
        statusEl.style.color = '#999';
    }
    // 发送清空事件到 APP
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({cmd: DRAW_EVENT, action: 'clear', from: 'web'}));
    }
}

window.addEventListener('resize', function () {
    if (drawInitialized) {
        resizeDrawCanvas();
    }
});
