function display() {
    document.getElementById("scanning-text").style.display = "inline";
    document.getElementById("devices-table").style.display = "block";
    document.getElementById("pair-button").style.display = "block";
}

function changeWindow() {
    document.getElementById("nav-bar").innerHTML = "Device Name"
    document.getElementById("scan-button").style.display = "none";
    document.getElementById("scanning-text").style.display = "none";
    document.getElementById("devices-table").style.display = "none";
    document.getElementById("pair-button").style.display = "none";
    document.getElementById("disconnect-button").style.display = "inline-block";
    document.getElementById("battery-icon").style.display = "inline-block";
    document.getElementById("heart-rate").style.display = "block";
    document.getElementById("user-status").style.display = "block";

}

function initialWindow() {
    document.getElementById("scan-button").style.display = "inline-block";
    document.getElementById("disconnect-button").style.display = "none";
    document.getElementById("battery-icon").style.display = "none";
    document.getElementById("heart-rate").style.display = "none";
    document.getElementById("user-status").style.display = "none";
}

function changeBatteryIcon(batteryLevel) {
    switch (batteryLevel) {
        case (batteryLevel > 60):
            document.getElementById("battery-icon").style.color = "green";
            break;
        case ((batteryLevel < 60) && (batteryLevel > 20)):
            document.getElementById("battery-icon").style.color = "yellow";
            break;
        case (batteryLevel < 20):
            document.getElementById("battery-icon").style.color = "red";
            break;
        default:
            break;
    }

}