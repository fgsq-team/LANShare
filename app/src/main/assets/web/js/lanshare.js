// ============================================================
// 全局变量
// ============================================================
var fastFile = {path: '/', isFile: false, name: '...', isDirectory: true};
var files = [fastFile, {path: '', isFile: false, name: '/'}];
var medias = [];
var apps = [];
var rootPath = '/';
var webName = '';
var tabPage = 1;
var defaultPage = 'apps';
var menuKeys = [];
var filesSelectCount = 0;

// ============================================================
// 主题管理
// ============================================================

function applyTheme(theme) {
    var resolvedTheme = theme;
    if (theme === 'follow_system') {
        resolvedTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    document.body.className = 'theme-' + resolvedTheme;
    localStorage.setItem('theme', theme);
}

function getThemeName(theme) {
    switch (theme) {
        case 'light':        return '浅色';
        case 'dark':         return '暗色';
        case 'emerald':      return '翠绿';
        case 'follow_system': return '跟随系统';
        default:             return theme;
    }
}

// 监听系统主题变化（跟随系统模式）
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
    if (localStorage.getItem('theme') === 'follow_system') {
        applyTheme('follow_system');
    }
});

// ============================================================
// 工具函数
// ============================================================

function truncateString(str, maxLen) {
    if (str.length > maxLen) {
        return str.slice(0, maxLen - 3) + '...';
    }
    return str;
}

function copyToClip(text) {
    var input = document.createElement('input');
    input.setAttribute('value', text);
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
    lightyear.notify('复制成功', 'success', 1000);
}

// ============================================================
// 初始化
// ============================================================

initConfig();

function initConfig() {
    post({
        url: '/initConfig',
        data: JSON.stringify({test: 1}),
        success: function (data) {
            rootPath = data.rootPath;
            webName = data.name;
            localStorage.setItem('token', data.token);

            // 应用主题：优先服务器推送，否则使用本地缓存
            if (data.theme) {
                applyTheme(data.theme);
            } else {
                var savedTheme = localStorage.getItem('theme');
                applyTheme(savedTheme || 'light');
            }

            if (data.pass) {
                if (data.menus && data.menus.length > 0) {
                    defaultPage = data.menus[0].key;
                    buildSidebar(data.menus);
                }
                // 初始化不保存消息设置
                applyChatNoSave();
                $('.lyear-layout-web').show();
                initWS();
                initView();
            } else {
                $('#myModal').modal('show');
                checkPass();
            }
        },
        error: function (err) {
            console.error(err.message);
        }
    });
}

function checkPass() {
    var timer = setInterval(function () {
        post({
            url: '/checkPass',
            data: {},
            success: function (data) {
                if (data.pass) {
                    clearInterval(timer);
                    location.reload();
                }
            },
            error: function (err) {
                console.error(err.message);
            }
        });
    }, 2000);
}

function initView() {
    $('.web-name').val(webName);
    var hash = window.location.hash.substring(1);
    // 提取当前 hash 对应的菜单 key
    var currentPage = '';
    if (hash) {
        var qIdx = hash.indexOf('?');
        var hashPath = qIdx >= 0 ? hash.substring(0, qIdx) : hash;
        // files 页面的 hash 格式为 nav-item-files/path，需要按前缀匹配
        if (hashPath.startsWith('nav-item-files')) {
            currentPage = 'files';
        } else {
            // 其他页面 hash 格式为 nav-item-xxx
            currentPage = hashPath.replace('nav-item-', '');
        }
    }
    // 当前 hash 对应已配置的菜单项，保持当前路径
    if (currentPage && menuKeys.indexOf(currentPage) !== -1) {
        locationHashChanged();
    } else {
        // 当前路径不在菜单中，切换到默认页面
        navTo(defaultPage);
    }
}

function updateWebName() {
    post({
        url: '/updateWebName',
        data: JSON.stringify({webName: webName}),
        success: function () {},
        error: function (err) {
            console.error(err.message);
        }
    });
}

// ============================================================
// 侧边栏导航
// ============================================================

function buildSidebar(menus) {
    var $nav = $('#sidebarNav');
    $nav.empty();
    menuKeys = [];
    menus.forEach(function (menu) {
        menuKeys.push(menu.key);
        $nav.append(
            '<li class="nav-item nav-item-' + menu.key + '" data-page="' + menu.key + '">' +
            '<a href="javascript:;" onclick="navTo(\'' + menu.key + '\')">' +
            '<i class="' + menu.icon + '"></i>' +
            '<span class="nav-text">' + menu.text + '</span>' +
            '</a></li>'
        );
    });
}

function setActiveNav(page) {
    $('.nav-drawer > li').removeClass('active');
    $('[data-page="' + page + '"]').addClass('active');
}

// 导航切换 - hash 变化时由 onhashchange 处理，hash 相同时直接调用
function navTo(page) {
    var oldHash = window.location.hash;
    switch (page) {
        case 'apps':
            window.location.hash = 'nav-item-apps';
            break;
        case 'media':
            window.location.hash = 'nav-item-media?index=-1';
            break;
        case 'files':
            window.location.hash = 'nav-item-files' + rootPath + '?isBack=false';
            break;
        case 'chat':
            window.location.hash = 'nav-item-chat';
            break;
        case 'draw':
            window.location.hash = 'nav-item-draw';
            break;
    }
    // hash 实际发生变化时由 onhashchange 事件处理，避免重复调用
    // hash 未变时（如首次加载或同一页面刷新）才直接调用
    if (window.location.hash === oldHash) {
        locationHashChanged();
    }
}

// ============================================================
// 页面路由 - 根据 hash 切换内容面板
// ============================================================

/** 隐藏所有内容面板并重置选中状态 */
function viewEmpty() {
    resetMediaSelect();
    // 退出聊天多选模式
    if (chatMultiSelectMode) {
        toggleChatMultiSelect(false);
    }
    $('.filelist').empty();
    $('.media-content').empty();
    $('.app-content').empty();
    document.body.scrollTop = document.documentElement.scrollTop = 0;
}

/** 统一切换内容面板的显示/隐藏 */
function showPanel(panelSelector) {
    $('.app-content, .file-content, .media-content, .chat-content, .draw-content').hide();
    $(panelSelector).show();
}

function locationHashChanged() {
    var hash = window.location.hash.substring(1);

    if (hash.startsWith('nav-item-apps')) {
        setActiveNav('apps');
        tabPage = 0;
        viewEmpty();
        showPanel('.app-content');
        appList();
        return;
    }

    if (hash.startsWith('nav-item-files')) {
        setActiveNav('files');
        tabPage = 1;
        viewEmpty();
        showPanel('.file-content');
        var prefix = 'nav-item-files';
        var qIdx = hash.indexOf('?');
        var filePath = qIdx > prefix.length ? hash.substring(prefix.length, qIdx) : rootPath;
        var isBack = qIdx >= 0 ? hash.substring(hash.lastIndexOf('=') + 1) === 'true' : false;
        openDir(isBack, decodeURIComponent(filePath));
        return;
    }

    if (hash.startsWith('nav-item-media')) {
        setActiveNav('media');
        tabPage = 2;
        var qIdx2 = hash.indexOf('?');
        var index = qIdx2 >= 0 ? (hash.substring(qIdx2 + 1).split('=')[1] - 0) : -1;
        viewEmpty();
        showPanel('.media-content');
        mediaList(index);
        return;
    }

    if (hash.startsWith('nav-item-draw')) {
        setActiveNav('draw');
        tabPage = 4;
        viewEmpty();
        showPanel('.draw-content');
        initDrawCanvas();
        return;
    }

    // 默认：聊天
    setActiveNav('chat');
    tabPage = 3;
    viewEmpty();
    showPanel('.chat-content');
    loadChatFromCache();
}

// 保留 hash 监听以支持浏览器前进后退
window.onhashchange = locationHashChanged;

// ============================================================
// 文件列表
// ============================================================

function openFile(fileName, filePath) {
    var requestUrl = '/file/' + fileName + '?path=' + encodeURIComponent(filePath) + '&token=' + localStorage.getItem('token');
    window.open(requestUrl, '_blank');
}

function dirClick(isBack, path) {
    window.location.hash = 'nav-item-files' + path + '?isBack=' + isBack;
}

function openDir(isBack, path) {
    post({
        url: '/files',
        data: JSON.stringify({path: path, isBack: isBack}),
        success: function (data) {
            viewEmpty();
            fastFile.path = data.path;
            $('.path-text').text(data.path);
            files = data.list;

            files.forEach(function (item, idx) {
                var li;
                // 第一个 item (idx===0) 固定为返回上一级，强制使用 dirClick
                if (idx === 0 || item.isDirectory) {
                    li = $('<div file-path="' + item.path + '" class="file-item" onclick="dirClick(\'' + (idx === 0) + '\',encodeURI($(this).attr(\'file-path\')))\"></div>');
                } else {
                    li = $('<div file-path="' + item.path + '" class="file-item" onclick="openFile(\'' + item.name + '\',$(this).attr(\'file-path\'))"></div>');
                }

                // 第一个 item 不添加 checkbox
                if (idx !== 0) {
                    li.append('<input name="checkbox" value="0" type="checkbox" onclick="event.cancelBubble=true;fileSelect(this)" class="file-img-select">');
                }

                // 文件夹图标（第一个 item 固定显示为返回上一级）
                if (idx === 0 || item.isDirectory) {
                    li.append('<div class="file-item-icon file-item-base file-icon-folder"></div>');
                } else {
                    li.append('<div class="file-item-icon file-item-base file-icon-file"></div>');
                }

                var info = $('<div class="file-item-info"></div>');
                info.append($('<div class="file-item-name">' + item.name + '</div>'));
                if (idx === 0) {
                    info.append($('<div class="file-item-time file-item-base"></div>'));
                } else {
                    info.append($('<div class="file-item-time file-item-base">' + item.time + '</div>'));
                }
                li.append(info);
                $('.filelist').append(li);
            });
            updateSelectAllToolbar('file');
            $('#fileSelectAllCheckbox').prop('checked', false);
        },
        error: function (err) {
            console.log(err.message);
        }
    });
}

// ============================================================
// 媒体列表
// ============================================================

function openMedia(elem) {
    if (filesSelectCount > 0) {
        var checkbox = $(elem).children('.media-img-select');
        checkbox.prop('checked', !checkbox.is(':checked'));
        mediaSelect(checkbox);
    } else {
        openFile($(elem).attr('file-name'), $(elem).attr('path'));
    }
}

function openMediaFolder(elem, folderIndex) {
    if (filesSelectCount > 0) {
        var checkbox = $(elem).children('.media-img-select');
        checkbox.prop('checked', !checkbox.is(':checked'));
        mediaSelect(checkbox);
    } else {
        openHash(folderIndex);
    }
}

function openHash(index) {
    window.location.hash = 'nav-item-media?index=' + index;
}

function mediaList(folderIndex) {
    $('.media-content').empty();
    post({
        url: '/media',
        type: 'post',
        data: JSON.stringify({folderIndex: folderIndex}),
        success: function (data) {
            medias = data;
            data.forEach(function (item) {
                var cardBox;
                if (item.isDirectory) {
                    cardBox = $('<div is-directory="true" m-index="' + item.index + '" class="media-dir cardBox media-item" onclick="openMediaFolder(this,' + item.index + ')"></div>');
                } else {
                    cardBox = $('<div is-directory="false" m-index="' + item.index + '" i-index="' + item.subIndex + '" file-name="' + item.name + '" path="' + item.path + '" class="media-file cardBox media-item" onclick="openMedia(this)"></div>');
                }
                cardBox.append($('<div class="media-img" data-url="/imageload/' + item.name + '?index=' + item.imgIndex + '&token=' + localStorage.getItem('token') + '"></div>'));
                cardBox.append('<input name="checkbox" value="0" type="checkbox" onclick=\'event.cancelBubble=true;mediaSelect(this)\' class="media-img-select">');
                if (item.isVideo) {
                    cardBox.append($('<div class="media-video-time">' + item.videoTime + '</div>'));
                }
                if (item.isDirectory) {
                    cardBox.append('<div>' + truncateString(item.name, 18) + '</div>');
                }
                $('.media-content').append(cardBox);
                mediaObserver.observe(cardBox[0]);
            });
            updateSelectAllToolbar('media');
            $('#mediaSelectAllCheckbox').prop('checked', false);
        },
        error: function (err) {
            console.error(err.message);
        }
    });
}

// ============================================================
// 应用列表
// ============================================================

function openApkFile(fileName, packageName) {
    var requestUrl = '/apkfile/' + fileName + '?packageName=' + packageName + '&token=' + localStorage.getItem('token');
    window.open(requestUrl, '_blank');
}

function appList() {
    $('.app-content').empty();
    post({
        url: '/apps',
        data: JSON.stringify({}),
        success: function (data) {
            apps = data.list;
            apps.forEach(function (app) {
                var appbox = $('<div class="appBox media-item" onclick="openApkFile(\'' + app.name + '\',\'' + app.packageName + '\')"></div>');
                appbox.append($('<div class="app-img" data-url="/appicon?packageName=' + app.packageName + '"/>'));
                appbox.append($('<div>' + app.name + '</div>'));
                appbox.append($('<div>' + app.length + '</div>'));
                appbox.append($('<img class="app-select" src="/drawable?name=ic_image_un_select"/>'));
                $('.app-content').append(appbox);
                appListObserver.observe(appbox[0]);
            });
        },
        error: function () {}
    });
}

// ============================================================
// 懒加载 - IntersectionObserver
// ============================================================

function handleMediaIntersection(entries, observer) {
    entries.forEach(function (entry) {
        if (entry.isIntersecting) {
            var $img = $(entry.target).find('.media-img');
            $img.css('background-image', 'url("' + $img.data('url') + '")');
            observer.unobserve(entry.target);
        }
    });
}

function handleApplistIntersection(entries, observer) {
    entries.forEach(function (entry) {
        if (entry.isIntersecting) {
            var $img = $(entry.target).find('.app-img');
            $img.css('background-image', 'url("' + $img.data('url') + '")');
            observer.unobserve(entry.target);
        }
    });
}

var lazyLoadOptions = {root: null, rootMargin: '0px', threshold: 0.5};
var mediaObserver = new IntersectionObserver(handleMediaIntersection, lazyLoadOptions);
var appListObserver = new IntersectionObserver(handleApplistIntersection, lazyLoadOptions);

// ============================================================
// 文件选择与下载
// ============================================================

function updateSelectCount() {
    if (filesSelectCount > 0) {
        $('.download-files').show();
    } else {
        $('.download-files').hide();
    }
    $('.download-files').text('下载文件(' + filesSelectCount + ')');
}

function mediaSelect(checkbox) {
    if ($(checkbox).is(':checked')) {
        filesSelectCount++;
    } else {
        filesSelectCount--;
    }
    updateSelectCount();
    updateSelectAllCheckbox('media');
}

function fileSelect(checkbox) {
    if ($(checkbox).is(':checked')) {
        filesSelectCount++;
    } else {
        filesSelectCount--;
    }
    updateSelectCount();
    updateSelectAllCheckbox('file');
}

function resetMediaSelect() {
    filesSelectCount = 0;
    $('.download-files').hide();
    $('#fileSelectAllToolbar, #mediaSelectAllToolbar').hide();
    $('#fileSelectAllCheckbox, #mediaSelectAllCheckbox').prop('checked', false);
}

function toggleSelectAll(type, checkbox) {
    var isChecked = $(checkbox).is(':checked');
    if (type === 'file') {
        $('.file-img-select').prop('checked', isChecked);
    } else {
        $('.media-img-select').prop('checked', isChecked);
    }
    filesSelectCount = isChecked ? (type === 'file' ? $('.file-img-select').length : $('.media-img-select').length) : 0;
    updateSelectCount();
}

function updateSelectAllCheckbox(type) {
    var $checkboxes = type === 'file' ? $('.file-img-select') : $('.media-img-select');
    var $selectAll = type === 'file' ? $('#fileSelectAllCheckbox') : $('#mediaSelectAllCheckbox');
    if ($checkboxes.length === 0) return;
    var allChecked = $checkboxes.length > 0 && $checkboxes.filter(':checked').length === $checkboxes.length;
    $selectAll.prop('checked', allChecked);
}

function updateSelectAllToolbar(type) {
    var $checkboxes = type === 'file' ? $('.file-img-select') : $('.media-img-select');
    var $toolbar = type === 'file' ? $('#fileSelectAllToolbar') : $('#mediaSelectAllToolbar');
    if ($checkboxes.length > 0) {
        $toolbar.show();
    } else {
        $toolbar.hide();
    }
}

function downloadFile() {
    var list = [];
    $('.file-item').each(function (idx, elem) {
        if ($(elem).find('.file-img-select').is(':checked')) {
            list.push($(elem).attr('file-path'));
        }
    });
    lightyear.notify('文件正在打包中...', 'info', 1000);
    lightyear.loading('show');
    post({
        url: '/compressFiles',
        data: JSON.stringify({list: list}),
        success: function (data) {
            lightyear.loading('hide');
            lightyear.notify('文件打包成功，开始下载', 'success', 1000);
            window.location.href = '/downloadZipFile?tempFile=' + data.tempFile;
        },
        error: function (err) {
            console.error(err.message);
            lightyear.loading('hide');
            lightyear.notify('打包文件失败', 'danger', 1000);
        }
    });
}

function downloadMedia() {
    var list = [];
    $('.media-item').each(function (idx, elem) {
        if ($(elem).find('.media-img-select').is(':checked')) {
            var isDirectory = $(elem).attr('is-directory');
            var index = $(elem).attr('m-index') - 0;
            var subIndex = isDirectory === 'true' ? -1 : ($(elem).attr('i-index') - 0);
            list.push({index: index, subIndex: subIndex, isDirectory: isDirectory === 'true'});
        }
    });
    lightyear.notify('文件正在打包中...', 'info', 1000);
    lightyear.loading('show');
    post({
        url: '/compressMedias',
        data: JSON.stringify({list: list}),
        success: function (data) {
            lightyear.loading('hide');
            lightyear.notify('文件打包成功，开始下载', 'success', 1000);
            window.location.href = '/downloadZipFile?tempFile=' + data.tempFile;
        },
        error: function (err) {
            console.error(err.message);
            lightyear.loading('hide');
            lightyear.notify('打包文件失败', 'danger', 1000);
        }
    });
}

function downloadFiles() {
    if (tabPage === 1) {
        downloadFile();
    } else if (tabPage === 2) {
        downloadMedia();
    }
}

// ============================================================
// 上传文件
// ============================================================

function showUploadDialog() {
    $('#select-file').val('');
    $('#displayFile').val('');
    $('#uploadFileDialog').modal('show');
}

function uploadFile(formData, onSuccess, onError) {
    var ajax = new XMLHttpRequest();
    ajax.open('POST', '/uploadFile', true);
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

// ============================================================
// 右键菜单
// ============================================================

function showContextMenu(x, y) {
    var $menu = $('.context-menu');
    $menu.css({display: 'block', left: x + 'px', top: y + 'px'});
    $menu.on('click', function () {
        hideContextMenu();
    });
}

function hideContextMenu() {
    $('.context-menu').css('display', 'none');
}

// ============================================================
// DOM Ready - 事件绑定
// ============================================================

$(function () {
    var $fileSelector = $('#select-file');

    // 文件选择对话框
    $('#displayFile').on('change', function (e) {
        $('#select-file').val($('#displayFile').val());
        var selectedFile = e.target.files[0];
        if (selectedFile) {
            uploadData = new FormData();
            uploadData.append('file', selectedFile);
            populateDeviceSelect();
            $('#uploadFileDialog').modal('hide');
            $('#devSelectDialog').modal('show');
        }
    });

    // 拖拽上传
    $(document).on('dragover', function (e) {
        $fileSelector.css('display', 'block');
        e.preventDefault();
    });

    $fileSelector.on('dragenter', function () {
        $fileSelector.attr('placeholder', '松手即可上传');
    });

    $fileSelector.on('dragleave', function () {
        $fileSelector.attr('placeholder', '拖拽到此处上传文件');
    });

    $fileSelector.on('drop', function (e) {
        $fileSelector.attr('placeholder', '点击选择文件或者拖拽文件到此处');
        $('#uploadFileDialog').modal('hide');
        uploadData = new FormData();
        Array.from(e.originalEvent.dataTransfer.files).forEach(function (file) {
            uploadData.append('file', file);
        });
        populateDeviceSelect();
        $('#uploadFileDialog').modal('hide');
        $('#devSelectDialog').modal('show');
        e.preventDefault();
    });

    // 右键菜单
    $(document).on('contextmenu', function (e) {
        e.preventDefault();
        var $target = $(e.target);

        // 聊天消息右键菜单
        var $chatBubble = $target.closest('.chat_left, .chat_right');
        if ($chatBubble.length > 0) {
            var $wrapper = $chatBubble.parent('.chat-item-wrapper');
            if ($wrapper.length > 0) {
                var msgId = $wrapper.data('msg-id');
                var $menu = $('.context-menu');
                $menu.empty();
                if (chatMultiSelectMode) {
                    // 多选模式下右键仅支持删除单条
                    $menu.append('<a class="menu-item" onclick="deleteChatMessage(\'' + msgId + '\')">删除</a>');
                } else {
                    $menu.append('<a class="menu-item" onclick="deleteChatMessage(\'' + msgId + '\')">删除</a>');
                    $menu.append('<a class="menu-item" onclick="toggleChatMultiSelect(true)">多选</a>');
                    $menu.append('<a class="menu-item" onclick="clearAllChatMessages()">清空所有消息</a>');
                }
                showContextMenu(e.clientX, e.clientY);
                return;
            }
        }

        if ($target.hasClass('media-file')) {
            var fileName = $target.attr('file-name');
            var filePath = $target.attr('path');
            var $menu = $('.context-menu');
            var requestUrl = window.location.origin + '/file/' + fileName + '?path=' + encodeURIComponent(filePath) + '&token=' + localStorage.getItem('token');
            $menu.empty();
            $menu.append('<a class="menu-item" onclick="openFile(\'' + fileName + '\',\'' + filePath + '\')">打开</a>');
            $menu.append('<a class="menu-item" onclick="copyToClip(\'' + requestUrl + '\')">复制链接</a>');
            showContextMenu(e.clientX, e.clientY);
        } else if ($target.hasClass('media-dir')) {
            var folderIndex = $target.attr('m-index');
            var $menu = $('.context-menu');
            $menu.empty();
            $menu.append('<a class="menu-item" onclick="openHash(' + folderIndex + ')">打开</a>');
            showContextMenu(e.clientX, e.clientY);
        }
    });

    // 点击空白处关闭右键菜单
    $(document).on('click', function () {
        hideContextMenu();
    });

    // 阻止 checkbox 点击事件冒泡
    $('.media-img-select').click(function (e) {
        e = window.event || e;
        if (e.stopPropagation) {
            e.stopPropagation();
        } else {
            e.cancelBubble = true;
        }
    });

    $('.file-icon-select').click(function (e) {
        e = window.event || e;
        if (e.stopPropagation) {
            e.stopPropagation();
        } else {
            e.cancelBubble = true;
        }
    });

    // 设备名称编辑
    $('.web-name').on('blur', function () {
        webName = $(this).val();
        updateWebName();
    });
});

/** 填充设备选择列表（上传文件时选择目标设备） */
function populateDeviceSelect() {
    var $select = $('.select_send_device');
    $select.empty();
    $select.append('<tr onclick="startSendFile(null)" class="select_send_device_item"><td>本机设备</td><td></td></tr>');
    for (var i = 0; i < onLineDeviceList.length; i++) {
        var device = onLineDeviceList[i];
        $select.append('<tr onclick="startSendFile(\'' + device.address + '\')" class="select_send_device_item"><td>' + device.devName + '</td><td>' + device.devIP + '</td></tr>');
    }
}
