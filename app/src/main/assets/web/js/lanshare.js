let fastFile = {
    path: "/",
    isFile: false,
    name: "...",
    isDirectory: true
}

let files = [fastFile, {
    path: "",
    isFile: false,
    name: "/"
}]

let medias = []
let rootPath = "/"
let apps = []

initConfig();

function initConfig() {
    $.ajax({
        url: "/initConfig",
        type: "post",
        dataType: "json",
        contentType: "json/application",
        data: JSON.stringify({"test": 1}),
        success: function (result) {
            rootPath = result.rootPath;
            initView();
        },
        error: function (result) {
            console.error(result.message)
        }
    });
}

function initView() {
    mediaList(-1)
    onfile(false, rootPath)
}

function openFile(name, path) {
    let requestUrl = "/file/" + name + "?path=" + path;
    window.open(requestUrl, "_blank");
}

function openApkFile(name, packageName) {
    let requestUrl = "/apkfile/" + name + "?packageName=" + packageName;
    window.open(requestUrl, "_blank");
}


function mediaList(index) {
    $(".media-content").empty();
    $.ajax({
        url: "/media",
        type: "post",
        dataType: "json",
        contentType: "json/application",
        data: JSON.stringify({folderIndex: index}),
        success: function (result) {
            medias = result;
            result.forEach(function (item, index) {
                let cardBox;
                if (item.isDirectory) {
                    cardBox = $("<div class=\"cardBox media-item\" onclick=\"mediaList(" + item.index + ")\"></div>");
                } else {
                    cardBox = $("<div class=\"cardBox media-item\" onclick=\"openFile('" + item.name + "','" + item.path + "')\"></div>");
                }
                cardBox.append($("<div class=\"media-img\" style=\"background-image: url('/imageload/"
                    + item.name + "?index=" + item.imgIndex + "');\"></div>"));
                cardBox.append("<div class='media-img-select'\n" +
                    "style=\"background-image: url('/drawable?name=ic_image_un_select');\"></div>");
                if (item.isDirectory) {
                    cardBox.append("<div>" + item.name + "</div>");
                }
                $(".media-content").append(cardBox);
            });
        },
        error: function (result) {
            console.error(result.message)
        }
    });
}


function appList() {
    $(".app-content").empty();
    $.ajax({
        url: "/apps",
        type: "post",
        dataType: "json",
        contentType: "json/application",
        data: JSON.stringify({}),
        success: function (result) {
            apps = result.list;
            apps.forEach(function (item, index) {
                let appbox = $("<div class='appBox media-item' onclick='openApkFile(\"" + item.name + "\",\"" + item.packageName + "\")'></div>");
                appbox.append($("<img class='app-img' src='/appicon?packageName=" + item.packageName + "'/>"))
                appbox.append($("<div>" + item.name + "</div>"))
                appbox.append($("<div>" + item.length + "</div>"))
                appbox.append($("<img class='app-select' src='/drawable?name=ic_image_un_select'/>"))
                $(".app-content").append(appbox);
            });
        },
        error: function (result) {
            // console.error(result)
        }
    });
}

function onfile(isBack, path) {
    $(".filelist").empty();
    $.ajax({
        url: "/files",
        type: "post",
        dataType: "json",
        contentType: "json/application",
        data: JSON.stringify({path: path, isBack: isBack}),
        success: function (result) {
            fastFile.path = result.path;
            // 设置路径显示
            $('.path-text').text(result.path);
            // 文件列表
            files = result.list;
            // // 插入首行的返回按钮
            // files.splice(0, 0, fastFile);
            let i = 0;
            // 循环遍历上面的json数据，每一行代表一个li
            files.forEach(function (item, index) {
                let li;
                if (item.isDirectory) {
                    li = $("<div class='file-item' onclick=\"onfile(" + (index === 0) + ",'" + item.path + "')\"></div>");
                } else {
                    li = $("<div class='file-item' onclick=\"openFile('" + item.name + "','" + item.path + "')\"></div>");
                }
                if (item.isDirectory) {
                    li.append("<div class='file-item-icon file-item-base file-icon-folder'></div>");
                } else {
                    li.append("<div class='file-item-icon file-item-base file-icon-file'></div>");
                }
                let info = $('<div class="file-item-info"></div>');
                info.append($("<div class='file-item-name'>" + item.name + "</div>"));
                if (index === 0) {
                    info.append($("<div class='file-item-time file-item-base'></div>"));
                } else {
                    info.append($("<div class='file-item-time file-item-base'>" + item.time + "</>"));
                }
                li.append(info);
                $(".filelist").append(li);
                i++
            });
        },
        error: function (result) {
            console.log(result.message)
        }
    });
}


$(".nav-item-media").click(function () {
    $(".filelist").empty();
    $(".file-list-div").hide();
    $(".media-content").show();
    mediaList(-1)
});

$(".nav-item-files").click(function () {
    $(".media-content").empty();
    $(".file-list-div").show();
    $(".media-content").hide();
    onfile(false, rootPath)
});

$(".nav-item-apps").click(function () {
    $(".media-content").empty();
    $(".filelist").empty();
    $(".app-content").show();
    $(".filelist").hide();
    $(".media-content").hide();
    appList()
});
