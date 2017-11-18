# Example 2: asynchronous requests
import asyncio
import requests

async def main():
    loop = asyncio.get_event_loop()
    futures = [
        loop.run_in_executor(
            None, 
            requests.get, 
            'http://132.207.12.99:8000/'
        )
        for i in range(40)
    ]
    for response in await asyncio.gather(*futures):
        pass

loop = asyncio.get_event_loop()
loop.run_until_complete(main())
