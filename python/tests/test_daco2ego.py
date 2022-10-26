#!/usr/bin/env python
from daco2ego import get_users, read_config, daco2_csv_to_list
from daco_user import User


def test_read_config():
    expected = {
        "client": {
            "base_url": "https://ego/v1/",
            "dac_api_url": "https://dac-api/v1"
        }
    }
    config = read_config("tests/test.conf")
    assert config == expected


def file_to_dict(name):
    with open("tests/" + name, "rt") as f:
        return daco2_csv_to_list(f.read())


def test_users():
    expected = [
        User('oicr@example.com', 'First Last', True, True),
        User('bettyGmail@example.com', 'Betty White', True, True),
        User('miami_gmail@example.com', 'Betty White', True, True),
        User('bettyGmail23@example.com', 'Betty White', True, True),
        User('blanche_gmail@example.com', 'Blanche Devereaux', True, True),
        User('wonderful@gmail.com', '&Aacute; wo&ntilde;derful user',
             True, True),
        User('a.random.guy.random@gmail.com', 'SOME RANDOM GUY',
             True, True),
        User('a.person@gmail.com', 'A Person', True, True)]

    users = file_to_dict("test.csv")
    print(users)
    assert users == expected
