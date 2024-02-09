“照片地图”工具可以用来将照片转换成地图。有必要带一张纸质地图作为备份，并验证 Trail Sense 的准确性。 这个工具是对其他地图应用与纸质地图的补充，充当将照片转化成地图的便捷方式，而不是前者的替代品。

## 创建地图
创建地图有三种方式：拍摄已有地图的照片、导入文件与生成空白地图。

1. 点按右下角的 '+' 按钮，选择导入地图偏好的方式：
    - **相机**：拍一张地图的照片，确保整张地图都在照片内。
    - **文件**：选择设备上的文件（JPG、PNG 或 PDF）。如果 PDF 文件包含地理空间数据，则会用于地图自动校准。
    - **空白**：根据指定位置与地图尺寸生成空白地图。默认情况下，地图以当前位置为中心；大小指的是地图中心到角落的距离。使用此选项时无需进一步校准。
2. 输入地图名称并点击“确定”。
3. 将剪裁框四角拖动到地图边缘，使照片大小与地图边界相吻合。你可以点击“预览”看到剪裁后的地图。请注意，在点击“下一步”后就无法修改裁剪范围。
4. 用两个已知位置校准地图：
    - 在提供的字段中输入现实世界位置，比如路标、路口或感兴趣的地点。
    - 在地图上点按选择对应位置。
    - 用“上一个”或“下一个”切换校准点。
    - 设置两个校准点后，点击“预览”查看校准后地图，地图上会显示附近的路径与信标。
    - 点击屏幕右上角的设置中心按钮，可以重新设置地图中心。
    - 使用双指缩放或右下角的缩放按钮来更加精确地选择位置。
    - 校准时会自动将地图朝北对齐，计算出的旋转数值会显示在顶部的地图名称下方。
    - 任何时候都可以点击“下一步”与“完成”来保存校准。
5. 轻按“完成”保存校准。

## 校准小提示

### 在徒步旅行时校准
- 第一点：选择拍摄地图所在的路标处，或者选择路口。
- 第二点：选择你遇到的第一个岔路口。如果没有，则选择地图上感兴趣的点，比如湖泊、山峰与地标。

### 用地图网格校准
如果你的地图有网格线：

1. 找到 UTM 区号，这是一个一位或两位数，后面紧跟一个字母（比如“16T”）。如果地图上没有，可以在 Trail Sense 的“转换”工具中输入大致纬度与经度来估算。比如，经度可以输入 10，纬度输入 45。
2. 找到地图边界的东经与北纬数值。地图上通常会有标记，有 3 到 6 或 7 位。如果数值只有三位，则在末尾补三个零（比如 123 对应 123000）。东经标记在地图顶部或底部，北纬在左侧或右侧。
3. 看地图上的网格线，找出你所在的网格区域。这些线代表所在位置的第一位数字（比如 123000E 与 234000N）。
4. 若要提高精确度，可以将网格沿横纵两个方向十等分。确定到所在位置距离最短的网格线。使用尺子（比如 Trail Sense 中的）以求精确度。举个例子，如果你的位置在距离网格右下角向左十分之二、向上十分之一的位置，新位置就是 123200E 与 234100N。
5. 在 Trail Sense 中输入 UTM 位置，并点击照片地图上的对应点。比如输入“16T 123200E 234100N”。
6. 对第二个校准点重复这些操作。若要力求精确，请使用距离第一个点较远的位置。

网格线的交叉点是最容易校准的。

### 用在线源校准
如果可以联网，你可以查找地图特征点（比如山峰与路口）的坐标。然后在 Trail Sense 点击地图上的相同位置并输入坐标。

### 寻找地理空间 PDF 文件
[CalTopo](https://caltopo.com) 是获取地理空间 PDF 的一个很好的来源，但网上也有许多其他的可供使用。

## 使用地图
创建地图后，你就可以将其用来导航了。地图会显示你的位置、路径与信标。

你可以拖动手指平移地图，伸缩双指或点击右下角的缩放按钮缩放地图。点击右上角的重新调整按钮，来重新调整屏幕上地图的位置。右下角会显示地图的比例尺。

点击右下角的 GPS 按钮可以以所在位置为中心移动地图。再点击一次会锁定所在位置与方向，最后点一次则会解锁。右上角的指南针图标总会指向北方。

默认情况下，地图会向北对齐，方向大致朝上，让地图正对屏幕。你可以禁用“设置 > 照片地图 > 保持地图正面朝上”，让地图（旋转到）向北对齐。注意：禁用此设置时，平移与缩放会有些偏差。

### 使用信标和导航功能
你创建的信标会显示在地图上。

When navigating to a beacon, the distance, direction, and estimated time of arrival (ETA) are displayed at the bottom. A line is drawn from your location to the beacon. To cancel navigation, click the 'X' button in the bottom-right.

You can initiate navigation from the map by tapping a beacon or long-pressing a map point and selecting 'Navigate'.

To create a beacon from the map, long-press a map point and choose 'Beacon.' This opens the 'Create Beacon' screen with the location filled in.

要进一步了解信标，请参见“信标”指南。

### 使用路径
你创建的路径会显示在地图上。

要从地图创建路径，请按照下文“测量图上距离”一节的说明操作。

要进一步了解路径，请参见“路径”指南。

## 测量图上距离
You can measure distances on a map by opening the map, clicking the menu button in the top-right, and selecting 'Measure' or 'Create path'. Tap the map to place markers, and the total distance will be displayed at the bottom. To undo the last marker, click the undo button in the bottom left. Cancel by clicking the 'X' button in the bottom-right. You can also convert the drawn path into a saved path by clicking the 'Create path' button at the bottom.

For a quick measurement from your location to a point, long-press that point on the map and click 'Distance'.

## 重新校准地图
To recalibrate a map, open the map, click the menu button in the top-right, and choose 'Calibrate'. Follow the instructions above to recalibrate.

## 改变地图投影
If your map points are not aligning correctly after calibrating (try calibrating again with different points first), consider changing the map projection. To do this, open the map, click the menu button in the top-right, and select 'Change projection'.

## 重命名地图
To rename a map, click the menu button on the map row you wish to rename, then select 'Rename' and provide a new name. Alternatively, open the map, click the menu button in the top-right, and choose 'Rename'.

## 删除地图
To delete a map, click the menu button on the map row you want to remove, then select 'Delete'. Alternatively, open the map, click the menu button in the top-right, and choose 'Delete'.

## 导出地图
To export a map, click the menu button on the map row you want to export, then select 'Export'. Alternatively, open the map, click the menu button in the top-right, and choose 'Export'. This action exports the map as a PDF, and if calibrated, it will convert it into a geospatial PDF.

## 打印地图
To print a map, click the menu button on the map row you want to print, then select 'Print'. Alternatively, open the map, click the menu button in the top-right, and choose 'Print'. This opens the system print dialog, enabling you to print the map.

## 改变地图分辨率
To alter the resolution of a Photo Map, click the menu button on the map row you want to adjust, then select 'Change resolution'. A dialog will appear, allowing you to switch between low (lowest quality and smallest file size), medium (moderate quality and file size), and high (highest quality and largest file size) resolutions. Keep in mind that changing the resolution is a permanent action and cannot be undone.

默认情况下，Trail Sense 在导入地图时会自动降低其分辨率。要改变此行为，请禁用“设置 > 照片地图 > 降低地图分辨率”。

## 组织地图
You can organize maps into groups. To create a group, click the '+' button in the bottom-right of the map list and select 'Group'. Give the group a name and click 'OK'. To add maps to the group, click on the group in the list and follow the map creation instructions. The map will be added to the chosen group.

To change the group of an existing map, click the menu button on the map row you want to move, select 'Move to', and choose the target group.

To rename a group, click the menu button on the group row you want to rename, then select 'Rename' and provide a new name.

You can delete a group (along with all maps within it) by clicking the menu button on the group row you wish to remove, then selecting 'Delete'.

## 搜索地图
To search through your created maps, use the search bar at the top of the map list. This search encompasses the current group and all subgroups. Additionally, you can sort maps by distance, time, or name by clicking the menu button in the top-right and selecting 'Sort'.

The preview of the map is displayed on the left side of the map row. You can disable this preview in Settings > Photo Maps > 'Show map previews'.
