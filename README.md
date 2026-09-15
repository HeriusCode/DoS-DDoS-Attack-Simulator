# DoS/DDoS Lab Traffic Simulator — Máy 1

Ứng dụng JavaFX này tạo **lưu lượng HTTP hợp lệ, giới hạn cứng** để kiểm thử máy chủ trong phòng lab. Nó không spoof IP, không tạo packet lỗi, không amplification/reflection và không làm việc với botnet.

## Kiến trúc tổng thể

`view` chỉ hiển thị và chuyển thao tác người dùng cho `AttackController`. Controller xác thực, điều phối simulator và đẩy snapshot `TrafficStatistics` lại giao diện. `simulation` tạo các logical worker có thể hủy; `network` chỉ thực hiện GET HTTP hợp lệ; `model` là dữ liệu bất biến/counter thread-safe; `logging` tập trung log.

## Class diagram (text)

```text
Main -> MainDashboard -> AttackController
AttackController -> DoSSimulator / DDoSSimulator -> SimulationWorker -> HttpClient
AttackController -> TargetServer, SimulationConfig, TrafficStatistics
AttackController -> SimulationLogger
MainDashboard -> TargetPanel, DoSSimulationPanel, DDoSSimulationPanel, StatisticsPanel
```

## Luồng hoạt động

1. Người dùng nhập target và nhấn **Test connection**. `ServerConnection` chỉ chấp nhận loopback hoặc IP private.
2. Nhấn Start: `AttackController` validate target/cấu hình, tạo statistics và simulator; không thể chạy hai mô phỏng cùng lúc.
3. Simulator tạo worker bằng `ScheduledExecutorService`; mỗi worker gửi GET theo rate và ghi success/fail/limited/latency.
4. JavaFX `Timeline` lấy statistics định kỳ, nên UI thread không thực hiện I/O mạng.
5. Hết thời gian hoặc Stop: controller hủy futures, shutdown executor, đóng HTTP client và đặt trạng thái COMPLETED/STOPPED.

## Cấu trúc thư mục

```text
src/main/java/client/{controller,simulation,network,model,view,logging}
```

## Chạy

```powershell
mvn javafx:run
```

Giới hạn an toàn: tối đa 3 logical nodes, 20 request/s/node, 60 giây. Target phải là `localhost`, `127.0.0.1`, `::1`, hoặc IPv4 private (10/8, 172.16/12, 192.168/16). Endpoint là `/`.
