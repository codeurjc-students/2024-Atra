import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class FormattingService {
  static formatDistance(value: number) {
    return value.toFixed(2) + "km"
  }

  static formatPace(value: number): string {
      const minutes = Math.floor(value / 60);
      const seconds = value % 60;
      console.log(value);

      return FormattingService.formatTime(value)//`${minutes}:${seconds.toFixed(0).padStart(2, '0')}`; // Format as mm:ss 1500 100 500
    }

  static formatTime(seconds: number, separator:number=0): string { //secsToHHMMSS
    //apparently new Date(seconds * 1000).toISOString().substring(11, 19) does the same thing, so long as total time is less than 24 hours

    // Total number of seconds in the difference
    const totalSeconds = Math.floor(seconds);
    // Total number of minutes in the difference
    const totalMinutes = Math.floor(totalSeconds / 60);
    // Total number of hours in the difference
    const totalHours = Math.floor(totalMinutes / 60);
    // Getting the number of seconds left in one minute
    const remSeconds = totalSeconds % 60;

    // Getting the number of minutes left in one hour
    const remMinutes = totalMinutes % 60;
    if (separator === 0) {
      const hoursString = totalHours != 0 ? totalHours.toString()+":":""
      const minsString = (remMinutes < 10 && totalHours!=0) ? "0"+remMinutes.toString():remMinutes.toString()
      const secsString = remSeconds < 10 ? "0"+remSeconds.toString():remSeconds.toString()

      return `${hoursString}${minsString}:${secsString}`
    }
    else if (separator === 1) {
      const hoursString = totalHours != 0 ? totalHours.toString()+"h ":""
      const minsString = remMinutes != 0 ? remMinutes.toString()+"m ":""
      const secsString = remSeconds.toString()+"s"

      return `${hoursString}${minsString}${secsString}`
    }
    else if (separator === 2) {
      const hoursString = totalHours != 0 ? totalHours.toString()+"h ":""
      const minsString = remMinutes != 0 ? remMinutes.toString()+"' ":""
      const secsString = remSeconds.toString()+"\""

      return `${hoursString}${minsString}${secsString}`
    }
    else {
      throw new Error(`Invalid separator value: ${separator}. Use 0 for HH:MM:SS or 1 for H:MM:SS.`)
    }
  }

  static formatDateTime(dateTime: Date): string {
    const hours = dateTime.getHours();
    const minutes = dateTime.getMinutes()<10 ? '0'+dateTime.getMinutes():dateTime.getMinutes()
    return `${hours}:${minutes}`
  }

  static toHoursMinsSecs(n: number){ //format should be H:MM:SS but this is fine for now
    const hours = Math.floor(n/3600)
    n = n%3600
    const mins = Math.floor(n/60)
    const secs = n%60

    const hoursString = hours != 0 ? hours+"h ":""
    const minsString = mins != 0 ? mins + "m ":""
    const secsString = secs + "s "


    return `${hoursString}${minsString}${secsString}`
  }
}
