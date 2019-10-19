# correct-tag

## Input example

````
|อุทยาน|แห่ง(org_start)|ชาติ|ลำน้ำ|น่าน(org_end)| |จ.|อุตรดิตถ์(loc)| |จัด|
|บริเวณ|กอง(loc_start)|หิน(loc)|แฟนตาซี(loc_end)| |ชั่วคราว|
````

## Output example

````
|อุทยาน|แห่ง(org_start)|ชาติ(org_cont)|ลำน้ำ(org_cont)|น่าน(org_end)| |จ.|อุตรดิตถ์(loc)| |จัด|
|บริเวณ|กอง(loc_start)|หิน(loc_cont)|แฟนตาซี(loc_end)| |ชั่วคราว|
````
