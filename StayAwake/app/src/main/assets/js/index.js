let timer;

function display() {
    window.Android.scanDevices();
    document.getElementById("scanning-text").style.display = "inline";
    let devTable = document.getElementById("devices-table"); 
    devTable.style.display = "block";

    let devices = [{name: "Mi Smart Band 6", mac: "FF:FF:FF:FF:FF:FF"}, {name: "Xiaomi Dev", mac: "CC:CC:CC:CC:CC:CC"}]; //window.Android.getDevices()
    let content = `<table class="highlight">
    <thead>
      <tr>
          <th>Devices found:</th>  
      </tr>
    </thead>
    <tbody>`

    devices.forEach((dev) => {
        content += `<tr>
        <td>${dev.name}</td>
        <td>${dev.mac}</td>
        <td><a id="${dev.name}/withid/${dev.mac}" onClick="authWindow(this.id)" class="waves-effect waves-light btn-large blue-grey lighten-1 right"><i class="material-icons right">bluetooth_connected</i>Pair</a></td>
      </tr>`
    });
    
    content += `</tbody>
    </table>`;

    devTable.innerHTML = content;
}

function authWindow(id) {
    let name = id.split("/withid/")[0];
    let mac = id.split("/withid/")[1];
    // console.log(id, name, mac)
    document.getElementById("nav-bar").innerHTML = `<div class="nav-wrapper blue-grey lighten-1 center-align" >
    <a href="#" class="brand-logo">${name}</a>
    </div>`
    document.getElementById("scan-button").style.display = "none";
    document.getElementById("scanning-text").style.display = "none";
    document.getElementById("devices-table").style.display = "none";
    document.getElementById("auth-input").style.display = "block";
    document.getElementById("auth-buttons").style.display = "block";
}

function changeWindow() {
    document.getElementById("scan-button").style.display = "none";
    document.getElementById("scanning-text").style.display = "none";
    document.getElementById("devices-table").style.display = "none";
    document.getElementById("disconnect-button").style.display = "inline-block";
    document.getElementById("battery-text").style.display = "inline-block";
    document.getElementById("heart-rate").style.display = "block";
    document.getElementById("chart").style.display = "block";
    document.getElementById("user-status").style.display = "block";
    document.getElementById("auth-input").style.display = "none";
    document.getElementById("auth-buttons").style.display = "none";

    monitor();

}

function initialWindow() {
    document.getElementById("nav-bar").innerHTML = `<div class="nav-wrapper blue-grey lighten-1 center-align" >
    <a href="#" class="brand-logo">Stay Awake</a>
    </div>`
    document.getElementById("scan-button").style.display = "inline-block";
    document.getElementById("disconnect-button").style.display = "none";
    document.getElementById("battery-text").style.display = "none";
    document.getElementById("heart-rate").style.display = "none";
    document.getElementById("chart").style.display = "none";
    document.getElementById("user-status").style.display = "none";
    document.getElementById("auth-input").style.display = "none";
    document.getElementById("auth-buttons").style.display = "none";
    document.getElementById("cancel-input").style.display = "none";
    document.getElementById("submit-input").style.display = "none";
    clearInterval(timer);
}

function changeBatteryIcon(batteryLevel) {
    document.getElementById("battery-text").innerHTML = `<i id="battery-icon" class="material-icons left font-size: medium" style="line-height: 0.5;">battery_std</i>` + String(batteryLevel) + "%";

    if (batteryLevel > 60) {
        document.getElementById("battery-icon").className = "material-icons left font-size: medium green-text";
        console.log(batteryLevel);
    } else if ((batteryLevel < 60) && (batteryLevel > 20)) {
        document.getElementById("battery-icon").className = "material-icons left font-size: medium yellow-text";
        console.log(batteryLevel);
    } else {
        document.getElementById("battery-icon").className = "material-icons left font-size: medium red-text";
        console.log(batteryLevel);
    }
}

function monitor() {
    timer = setInterval(() => {
        let date = new Date();
        let value = 80 + (date.getTime() % 15);
        // let value = window.Android.getHeartRate()
        document.getElementById("heart-rate-text").innerHTML = String(value);
        changeBatteryIcon(date.getTime() % 100);
    }, 3000);
}