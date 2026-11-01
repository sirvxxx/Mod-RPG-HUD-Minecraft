# Custom RPG HUD (Fabric, client-side)

HUD kiri atas: wajah skin player aktif, nama, HP (angka + bar), hunger, armor.

## Cara dapat .jar tanpa install apa pun (GitHub Actions)
1. Buat repo baru di github.com (kosong), lalu upload SEMUA isi folder ini (termasuk folder `.github`).
2. Buka tab **Actions** -> workflow "Build mod" jalan otomatis (~3-5 menit).
3. Klik run yang selesai -> bagian **Artifacts** -> download `custom-rpg-hud-mc26.1.2` (atau `mc26.2`).
4. Taruh jar di `.minecraft/mods/` bersama Fabric API.

## Build lokal
Butuh JDK 25 dan Gradle 9.5.1+: `./build.sh` (atau `build.bat`). Hasil: `build/libs/custom-rpg-hud-*.jar`.

## Kustomisasi in-game
Tombol H atau /rpghud gui membuka menu pengaturan.
/rpghud x <angka>  /rpghud y <angka>  /rpghud scale <0.5-3>
/rpghud hidevanilla <true|false>  /rpghud toggle  /rpghud reset
Atau edit `.minecraft/config/custom_rpg_hud.json`.

## Versi lain (mis. 26.3)
Cek versi Fabric API di https://fabricmc.net/develop lalu tambah entri di matrix `.github/workflows/build.yml`
atau jalankan `./build.sh <mc_version> <fabric_api_version>`.
Jika ada error kompilasi karena nama API berubah antar versi, kebanyakan ada di `SkinHelper.java`.
